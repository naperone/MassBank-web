#!/bin/sh

printf "use strict;
package MassBank::Credentials;
use base 'Exporter';
our @EXPORT = {'getCredentials'}

sub getCredentials {
\tmy @foo;
\tmy $User;
\tmy $PassWord;
\topen(F, \"$CREDPATH/password.sh\");
\t@foo = grep(/USER/,<F>);
\tclose(F);
\tforeach (@foo) {
\t\tchomp;
\t\t\$User = ((split(\"=\"))[1]);
\t}
\topen(F, \"$CREDPATH/password.sh\");
\t@foo = grep(/PW/,<F>);
\tclose(F);
\tforeach (@foo) {
\t\tchomp;
\t\t\$PassWord = ((split(\"=\"))[1]);
\t}
\t%res = (\"PassWord\",\$PassWord,\"User\",\$User);
\treturn \$res;
}

1;" > Credentials.pm