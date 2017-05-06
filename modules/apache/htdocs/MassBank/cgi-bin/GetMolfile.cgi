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
# Molfile情報一括取得
#
# ver 1.0.0  2009.10.15
#
#-------------------------------------------------------------------------------
use CGI;
use File::Basename;
use lib dirname (__FILE__);
use Credentials;
use DBI;

print "Content-Type: text/plain\n\n";

my $query = new CGI;
my $names = $query->param('names');
my $DbName = $query->param('dsn');
open(F, "DB_HOST_NAME");
while ( <F> ) {
	chomp;
	$Host .= $_;
}
my $DB = "DBI:mysql:$DbName:$Host";

%credentials = getCredentials();
$User = $credentials{User};
$PassWord = $credentials{PassWord};

my $MolDir = "../DB/molfile/$DbName";
my $dbh = DBI->connect($DB, $User, $PassWord) || &errorexit;
my @name_list = split( '@', $names );
my $in = "NAME in(";
foreach my $name ( @name_list ) {
	$name =~ s/\'/\'\'/g;
#	print $name . "\n";
	$in .= "'$name',";
}
chop $in;
$in .= ")";
#print "select FILE, NAME from MOLFILE where $in\n";
@ans = &MySql("select FILE, NAME from MOLFILE where $in");
foreach $x ( @ans ) {
	($fname, $name) = @$x;
	open(F, "$MolDir/$fname.mol");
	@mol = ();
	while ( <F> ) {
		push(@mol, $_);
	}
	close(F);
	print "---NAME:$name\n";
	foreach $x ( @mol ) {
		print "$x";
	}
}
$dbh->disconnect;
exit(0);

sub errorexit() {
	print "-1\n";
	exit(0);
}

sub MySql() { local($sql) = @_;
	local($sth, $n, $i, @ans, @ret);
	@ret = ();
	$sth = $dbh->prepare($sql) || &errorexit;
	$sth->execute || &errorexit;
	$n = $sth->rows;
	for ( $i = 0; $i < $n; $i ++ ) {
	@ans = $sth->fetchrow_array;
	push(@ret, [@ans]);
	}
	$sth->finish || &errorexit;
	return @ret;
}

1;
