#!/usr/bin/perl

use lib '/Users/akaan/perl5/lib/perl5';
use warnings;
use strict;
use diagnostics;
use JSON;

#our $CURL_OPTS = '-u 751818:26c2558a20790eec2994096c3c455b43 https://jenkins.prod.cloud.fedex.com:8443/jenkins';
#my @list = qw(SEFS_FXF-7499/FXF_SEFS_CORE SEFS_FXF-7499/FXF_SEFS_SC_PROPERTIES);
#
#foreach my $r (@list) {
#	my @r = split/\//,$r;
#	my $path =  sprintf "curl -O $CURL_OPTS/job/ShipmentEFS/job/%s/config.xml", join("/job/",@r);
#	my $jobname = pop @r;
#	
#	#  curl -u 751818:26c2558a20790eec2994096c3c455b43 https://jenkins.prod.cloud.fedex.com:8443/jenkins/
#	#		createItem?name=ShipmentEFS_New/SEFS_FXG-6873/FXG_SEFS_DOMAIN_VIEW/FXG_SEFS_UTILITIES
#	#  --data-binary @config.xml -H "Content-Type:text/xml"
#	my $path_target =  sprintf "curl $CURL_OPTS/job/ShipmentEFS_New/job/%s/createItem?name=%s --data-binary \@config.xml -H \"Content-Type:text/xml\"", join("/job/",@r), $jobname;
#	my $path_delete =  sprintf "curl -X POST $CURL_OPTS/job/ShipmentEFS_New/job/%s/doDelete", join("/job/",@r);
#	print $path, "\n";
#	system($path);
#	print $path_target, "\n";
#	#system($path_target);
#	print $path_delete, "\n";
#}


my @x = ();

push @x, {
'desc' => 'CORE & DOMAIN (default).',
'property-file' => '*.*',
'envvar' => 'CODE',
'code' => '0DCAEWJO'
};
push @x, {
'desc' => 'CORE & DOMAIN w/o AS.',
'property-file' => 'C_D_WO_AS',
'envvar' => 'CODE',
'code' => '0DC0EWJO'
};
push @x, {
'desc' => '_____CORE_____',
'property-file' => 'NA',
'envvar' => 'CODE',
'code' => '000000000'
};
push @x, {
'desc' => 'CORE All.',
'property-file' => 'C_ALL',
'envvar' => 'CODE',
'code' => '00CAEWJO',
};
push @x, {
'desc' => 'CORE All w/o AS.',
'property-file' => 'C_D_WO_AS',
'envvar' => 'CODE',
'code' => '00C0EWJO',
};

push @x, {
'desc' => 'CORE BE engines only.',
'property-file' => 'C_D_BEO',
'envvar' => 'CODE',
'code' => '00C0E000',
};
push @x, {
'desc' => 'CORE BW engines only.',
'property-file' => 'C_BWO',
'envvar' => 'CODE',
'code' => '00C00W00',
};
push @x, {
'desc' => 'CORE AS only.',
'property-file' => 'C_AS',
'envvar' => 'CODE',
'code' => '00CA0000',
};
push @x, {
'desc' => 'CORE Java.',
'property-file' => 'C_JAVA',
'envvar' => 'CODE',
'code' => '00C0EWJO',
};
push @x, {
'desc' => '____DOMAIN____',
'property-file' => 'NA',
'envvar' => 'CODE',
'code' => '00000000',
};
push @x, {
'desc' => 'DOMAIN All..',
'property-file' => 'D_ALL',
'envvar' => 'CODE',
'code' => '0D00EWJO',
};
push @x, {
'desc' => 'DOMAIN BE engines only.',
'property-file' => 'D_BE',
'envvar' => 'CODE',
'code' => '0D00E000',
};
push @x, {
'desc' => 'DOMAIN JAVA only.',
'property-file' => 'D_JAVA',
'envvar' => 'CODE',
'code' => '00C000J0',
};
push @x, {
'desc' => '___OTHER___',
'property-file' => 'NA',
'envvar' => 'CODE',
'code' => '00000000',
};
push @x, {
'desc' => 'ADMIN Persistent Files',
'property-file' => 'O_ADMIN',
'envvar' => 'CODE',
'code' => 'A000000O',
};

my $str=JSON->new->utf8->pretty(0)->encode(\@x);
$str =~ s/"/\\"/g;
print $str;
