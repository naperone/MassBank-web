use strict;
package Credentials;
# use base 'Exporter';
# our @EXPORT = {'getCredentials'};
require Exporter;
our @ISA = qw(Exporter);
our @EXPORT = qw(getCredentials);

sub getCredentials {
	my @foo;
	my $User;
	my $PassWord;
	open(F, "/vagrant/password.sh");
	@foo = grep(/USER/,<F>);
	close(F);
	foreach (@foo) {
		chomp;
		$User = ((split("="))[1]);
	}
	open(F, "/vagrant/password.sh");
	@foo = grep(/PW/,<F>);
	close(F);
	foreach (@foo) {
		chomp;
		$PassWord = ((split("="))[1]);
	}
	my %res = ("PassWord",$PassWord,"User",$User);
	return %res;
}

1;	