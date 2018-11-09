#!/usr/bin/perl

use warnings;
use strict;
use diagnostics;

use Getopt::Std;
use Data::Dumper;
use XML::Simple;
use HTTP::Tiny;

sub _value {
    my ( $r, $k ) = @_;
    my $_v = $r->{$k} ? $r->{$k}->[0] : $r->{'parent'}->[0]->{$k}->[0];
    $_v =~ s/-SNAPSHOT//g;
    return $_v;
}
#
#
#
my %options = ( 'p' => 'pom.xml' );
getopts( 'p:n:g:a:v', \%options );
my $xs      = XML::Simple->new( ForceArray => 1 );
my $verbose = $options{'v'};
my %h       = ();
if ( $options{'n'} ) {
    %h = (
    'groupId'    => $options{'g'} ? $options{'g'} : 'groupId',
    'artifactId' => $options{'a'} ? $options{'a'} : 'artifactId',
    'version'    => $options{'n'}
    );
}
else {
    print STDERR "Loading $options{'p'}\n";
    my $e = $xs->XMLin( $options{'p'} ) or die "error: $?";
    if ($verbose) {
        print STDERR Dumper($e);
    }
    %h = (
    'groupId'    => _value( $e, 'groupId' ),
    'artifactId' => _value( $e, 'artifactId' ),
    'version'    => _value( $e, 'version' )
    );
}

$h{'groupId'} =~ s/\./\//g;
$h{'version.read'} = sprintf '%s', $h{'version'};
$h{'minorversion'} = sprintf '%s', $h{'version'};
$h{'majorversion'} = sprintf '%s', $h{'version'};
$h{'version'} =~ s/-SNAPSHOT//g;

#$h{'minorversion'} =~ s/\.\d+$//g;
( $h{'minorversion'} ) = $h{'minorversion'} =~ m!(\d+\.\d+).+!xmi;
( $h{'majorversion'} ) = $h{'majorversion'} =~ m!(\d+).+!xmi;
$h{'version.regex'} = "$h{'version'}";
$h{'version.regex'} =~ s/[0-9]/\\d\+/g;
$h{'minorversion.regex'} = "$h{'minorversion'}\\.";
$h{'minorversion.regex'} =~ s/[0-9]/\\d\+/g;
$h{'majorversion.regex'} = "$h{'majorversion'}\\.";
$h{'majorversion.regex'} =~ s/[0-9]/\\d\+/g;

print STDERR Dumper( \%h );

# Download metadata when reading from pom.xml
my @lines     = ();
my @v         = ();
my $last      = "";
my $lastmajor = "";
my $lastminor = "";
my $m         = undef;

# defaulting by default....
$last      = $h{'version'};
$lastmajor = $h{'majorversion'};
$lastminor = $h{'minorversion'};

# if the version number/name is not hard coded and supplied by -n option read the metadata
if ( not $options{'n'} ) {
    my $nexusMetadataUrl =
    sprintf
    'http://sefsmvn.ute.fedex.com:9999/nexus/content/repositories/releases/%s/%s/maven-metadata.xml',
    $h{'groupId'}, $h{'artifactId'};
    printf STDERR "requesting: $nexusMetadataUrl \n";
    my @lines = ();
    open( PS, "curl -s '$nexusMetadataUrl' | iconv -f iso8859-1 -t utf-8 |" )
    || die "Failed: $!\n";
    while (<PS>) {
        chomp;
        push @lines, $_;
    }
    close(PS);
    
    unless (@lines) {
        printf STDERR "no response : failed request, defaulting\n";
    }
    else {
        my $content = join( "", @lines );
        eval { $m = $xs->XMLin($content); };
        if ($@) {
            printf STDERR "no response : failed request, defaulting\n";
            $last = $h{'version'}      unless $last;
            $last = $h{'minorversion'} unless $last;
        }
        else {
            print STDERR "read $#lines lines of maven-metadata.\n";
            if ($verbose) {
                print STDERR Dumper( \@lines );
            }
            print STDERR "execute_returns: ", Dumper($m) if $verbose;
            my $version_arr =
            $m->{'versioning'}->[0]->{'versions'}->[0]->{'version'};
            foreach my $l ( @{$version_arr} ) {
                if (    $l =~ /^$h{'minorversion'}/
                and $l =~ /^$h{'version.regex'}/
                and $l !~ /%0A/ )
                {
                    $last = $l;
                    if ( $l =~ /^$h{'majorversion'}/ ) {
                        $lastminor = $l;
                    }
                    push @v, $l;
                }
                
                #print $l, "\n";
            }
            print STDERR "listed versions=", join( ",", @v ), "\n";
        }
    }
}
printf STDERR "major=%s, minor=%s, v=%s (%s)\n", $lastmajor, $lastminor, $last, @v;
if ($verbose) {
    print STDERR Dumper( \%h );
    print STDERR "last = $last, numberofv=", $#v, "\n";
}

# if there are no listed releases default to $last

$last = $lastminor if ( !$last );
if ( $last eq '' ) {
    printf "%s", $h{'version'};
}
elsif ($last eq -1 ) {
    
}
else {
    print STDERR "parsing='$last'\n" if $verbose;
    
    # at this point one should have version picked for incrementing
    my @inc = $last =~ m!^([0-9\.]+)\.(\d+)(\.\D+)$!xmi;
    print STDERR Dumper(\@inc);
    if ($#v >= 0) {
        unless (@inc) {
            @inc = $last =~ m!^(\d+\.\d+)\.(\d+)\S*!xmi;
        }
        print STDERR "inc: ", Dumper( \@inc );
        $inc[1]++
        if $m;    # only increment when there was metadata succesfully parsed.
        printf '%s.%d%s', $inc[0], $inc[1], $inc[2] ? $inc[2] : '';
    }
    else {
        printf '%s', $last;
    }
}

1;
