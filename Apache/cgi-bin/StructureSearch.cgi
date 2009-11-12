#! /usr/bin/perl
#-------------------------------------------------------------------------------
#
# Copyright (C) 2008 JST-BIRD MassBank
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
#
#===============================================================================
#
# 部分構造検索クライアント
#
# ver 1.0.6 2009.01.08
#
#-------------------------------------------------------------------------------
use CGI;
use DBI;
use Socket;
use File::Temp();

my $BASE_PATH = "../StructureSearch/";
# テンポラリファイル格納パス
my $TMP_PATH = $BASE_PATH . "temp/";
print "Content-Type: text/plain\n\n";

#------------------------------------------------
# リクエストパラメータ取得
#------------------------------------------------
my $query = new CGI;
my @params = $query->param();
my @moldata = ();
my @mz_list = ();
my $tol = "";
my $pi_check = 1;
my @inst_list = ();
my $ion = "1";
foreach my $key ( @params ) {
	my $val = $query->param($key);
	if ( $val ne '' ) {
		if ( index($key, 'moldata') >= 0 ) {
			push(@moldata, $val);
		}
		elsif ( index($key, 'mz') >= 0 ) {
			push(@mz_list, $val);
		}
		elsif ( $key eq 'tol' ) {
			$tol = $val;
		}
		elsif ( $key eq 'pi_check' ) {
			$pi_check = $val;
		}
		elsif ( $key eq 'inst') {
			@inst_list = $query->param($key);
		}
		elsif ( $key eq 'ion' ) {
			$ion = $val;
		}
	}
}

# DB名セット
my $db_name = $query->param('dsn');
if ( $db_name eq '' ) {
	$db_name = "MassBank";
}
# DBホスト名取得
open(F, "DB_HOST_NAME");
my $host_name = "";
while ( <F> ) {
	chomp;
	$host_name .= $_;
}
close(F);

my $port_no = "50000";
if ( open(F, $BASE_PATH . "SOCKET_PORT") ) {
	while ( <F> ) {
		if ( $_ =~ /^([0-9]*)$/ ) {
			$port_no = $_;
		}
		last;
	}
	close(F);
}


#------------------------------------------------
# molfileデータをテンポラリファイルに保存
#------------------------------------------------
my @qfname_list = ();
foreach my $data (@moldata) {
	my $TMP = File::Temp->new( DIR => $TMP_PATH,
		TEMPLATE => 'query_XXXXXX',
		SUFFIX => '.mol',
		UNLINK => 0 )
		or die( "temp file create error : $!\n");
	my $file_path = $TMP->filename;
	$TMP->print($data) or die( "temp file write error : $!\n");
	$TMP->close();
	push(@qfname_list, $file_path);
}


#------------------------------------------------
# パラメータセット
#------------------------------------------------
# クエリファイル数セット
my $query_num = scalar(@qfname_list);
my $query = "";
# クエリファイル名セット
foreach my $file_path ( @qfname_list ) {
	my $file_name = $file_path;
	my $pos = -1;
	my $separator = '';
	if ( $file_path =~ /\\/ ) {
		$separator = '\\';
	}
	elsif ( $file_path =~ /\//  ) {
		$separator = '/';
	}
	if ( $separator ne '' ) {
		$pos = rindex( $file_path, $separator );
		if ( $pos >= 0 ) {
			$file_name = substr( $file_path, $pos+1, length($file_path)-($pos+1) );
		}
	}
	$query .= "\t$file_name";
}

my $param = "$pi_check\t$query_num$query\t$db_name\n";
#------------------------------------------------
# UNIXドメインソケット作成
#------------------------------------------------
my $iaddr = inet_aton("localhost")
		or die("no host : $!\n");

my $sock_addr = pack_sockaddr_in($port_no, $iaddr);

socket(SOCK, PF_INET, SOCK_STREAM, 0)
		or die("socket error : $!\n");

#------------------------------------------------
# ソケット接続
#------------------------------------------------
connect(SOCK, $sock_addr)
		or die("connect error : $!\n");

#------------------------------------------------
# サーバへパラメータを送信
#------------------------------------------------
autoflush SOCK 1;
print SOCK $param;

#------------------------------------------------
# サーバからレスポンス(ヒットしたIDリスト)を受信
#------------------------------------------------
my $res = "";
while (<SOCK>) {
	$res .= $_;
}
# ソケットを閉じる
close(SOCK);

#------------------------------------------------
# テンポラリファイル削除
#------------------------------------------------
foreach my $file_path ( @qfname_list ) {
	unlink($file_path);
}

#------------------------------------------------
# レスポンスをタブ区切りで分解
#------------------------------------------------
if ( $res eq '' ) {
	exit;
}
# 最後のタブを取り除く
$res = substr($res, 0, length($res) - 1);
my @molname_list = split('\t', $res);
if ( scalar(@molname_list) < 1 ) {
	exit;
}

#------------------------------------------------
# DB接続
#------------------------------------------------
my $DB = "DBI:mysql:$db_name:$host_name";
my $User = 'bird';
my $PassWord = 'bird2006';
my $dbh = DBI->connect($DB, $User, $PassWord) or die "DB connect error \n";

#------------------------------------------------
# Peak Searchパラメータセット
#------------------------------------------------
my @mz_range = ();
my $mz_num = $#mz_list;
for my $i ( 0 .. $mz_num ) {
	my $mz = $mz_list[$i];
	my $min = $mz - ($tol + 0.00001);
	my $max = $mz + ($tol + 0.00001);
	push(@mz_range, "MZ between $min and $max");
}

#------------------------------------------------
# 化合物名をキーにしてレコードIDを取得
#------------------------------------------------
my $sql = "";
my @rec = ();
my @id_list = ();
foreach my $molname ( @molname_list ) {
	$sql = "select S.ID from MOLFILE M, SPECTRUM S where M.FILE = '$molname' "
			 . "and M.NAME = substring(S.NAME,1,instr(S.NAME,';')-1)";
	@rec = &MySql($sql);
	foreach my $item ( @rec ) {
		push(@id_list, $$item[0]);
	}
}

#------------------------------------------------
# INSTRUMENT_NO取得
#------------------------------------------------
my $inst_no = "";
my $isFound = true;
if ( $#inst_list != -1 ) {
	my @inst_list2 = ();
	foreach my $inst ( @inst_list ) {
		push(@inst_list2, "'$inst'");
	}
	my $in = join(",", @inst_list2);
	$sql = "select INSTRUMENT_NO from INSTRUMENT where INSTRUMENT_TYPE in($in)";
	@rec = &MySql($sql);
	if ( $#rec == -1 ) {
		$isFound = false;
	}
	else {
		my @inst_no_list = ();
		foreach my $item ( @rec ) {
			push(@inst_no_list, $$item[0]);
		}
		$inst_no = join(",", @inst_no_list);
	}
}

if ( $isFound eq true ) {
	#------------------------------------------------
	# SPECTRUM, RECORDテーブルより項目を取得
	#------------------------------------------------
	foreach my $id ( @id_list ) {
		# Peak Search
		if ( $mz_num >= 0 ) {
			$sql = "select count(MZ) from PEAK where ID = '$id'";
			foreach my $range ( @mz_range ) {
				$sql .= " and exists(select MZ from PEAK where ID = '$id' and $range)";
			}
			@rec = &MySql($sql);
			if ( int($rec[0][0]) == 0 ) {
				next;
			}
		}

		$sql = "select S.NAME, S.ID, S.ION, R.FORMULA, R.EXACT_MASS "
				 . "from SPECTRUM S, RECORD R where S.ID = '$id' and S.ID = R.ID";

		if ( $inst_no ne '' ) {
			$sql .= " and INSTRUMENT_NO in($inst_no)";
		}
		if ( $ion != 0 ) {
				 $sql .= " and S.ION = $ion";
		}
		@rec = &MySql($sql);
		foreach my $rec ( @rec ) {
			print join("\t", @$rec), "\n";
		}
	}
}

$dbh->disconnect;
exit;

sub MySql() {
	my $sql = $_[0];
	my @ans = ();
	my @ret = ();
	my $sth = $dbh->prepare($sql);
	$sth->execute or exit(0);
	my $n = $sth->rows;
	for ( my $i = 0; $i < $n; $i ++ ) {
		@ans = $sth->fetchrow_array;
		push(@ret, [@ans]);
	}
	$sth->finish;
	return @ret;
}
