#!/usr/bin/env python

import os
import sys
import getopt
import urllib2
import urlparse
import httplib
import shutil
import install
import itertools
import zipfile
import socket
import base64
import ssl
import shutil
import logging
import xml.etree.ElementTree as ET
LOGGER = logging.getLogger('installer')

NEXUSv3='https://nexus.prod.cloud.fedex.com:8443'
NEXUSv2='http://sefsmvn.ute.fedex.com:9999'


def elvis(var, default, fd=None):
    value = default
    if fd is not None:
        try:
            value = os.environ[str(var)] if str(var) in os.environ.keys() else default
        except:
            value = default
        fd[var] = value
    else:
        return var if var is not None else default


def __download(url, gavstring, fileName=None):
    def getFileName(url, openUrl):
        if 'Content-Disposition' in openUrl.info():
            # If the response has Content-Disposition, try to get filename from it
            cd = dict(map(
                lambda x: x.strip().split('=') if '=' in x else (x.strip(), ''),
                openUrl.info()['Content-Disposition'].split(';')))
            if 'filename' in cd:
                filename = cd['filename'].strip("\"'")
                if filename:
                    return filename
        # if no filename was found above, parse it out of the final URL.
        return os.path.basename(urlparse.urlsplit(openUrl.url)[2])
    r = None
    try:
        request = urllib2.Request(url)
        #if "https://" in url:
        try:
            ctx = ssl.create_default_context()
            ctx.check_hostname = False
            ctx.verify_mode = ssl.CERT_NONE
            r = urllib2.urlopen(request, context=ctx)
        except:
            r = urllib2.urlopen(request)

        #base64string = base64.b64encode('%s:%s' % ('sefsdev', 'sefsdev'))
        #request.add_header("Authorization", "Basic %s" % base64string)
        fileName = fileName or getFileName(url, r)
        with open(fileName, 'wb') as f:
            shutil.copyfileobj(r, f)
    except urllib2.HTTPError as err:
        install.LOGGER.info("failed to download: %s (reason:%s)" % (gavstring, err))
        return None
    finally:
        if r:
            r.close()
    return fileName


def download_metadata_version(url,packaging,version):
    install.LOGGER.debug(" retrieve meta data  : %s" % (url))
    filename = __download(url, url)
    install.LOGGER.info(" retrieved meta data [%s]: %s" % (packaging,filename))
    lines=[]
    tree = ET.parse(filename)
    resolved = version
    for versions in tree.findall('.//snapshotVersion' ):
        i=0
        install.LOGGER.info(versions)
        __version = versions.find('value').text
        __pack = versions.find('extension').text
        #install.LOGGER.info("pa k: %s / %s -- eq = %s" % ( __pack, packaging, __pack  == packaging))
        #print __version, resolved
        if __pack  == packaging:
            resolved = __version
    return resolved

''' download '''
def download(svr, repo, *argv):
    install.LOGGER.info("--> repo %s -> %s %s" % (repo, type(argv), str(argv)))
    outputFile = None
    if (type(argv) == tuple):
        argv = list(itertools.chain.from_iterable(argv))
    argv.reverse()
    gavstring = ":".join(argv)
    install.LOGGER.info("--->%s" % (gavstring))
    groupId = argv.pop()
    artifactId = argv.pop()
    version = None
    packaging = None
    if len(argv) > 0:
        version = argv.pop()
        if not '-SNAPSHOT' in version:
            repo = 'releases'
        if "nexus.prod.cloud.fedex.com" in svr and 'SEFS-6270' not in repo:
            repo = "SEFS-6270-%s" % (repo)
    if len(argv) > 0:
        packaging = argv.pop()
    # Nexus v2
    URL_PATTERN='%s/nexus/service/local/artifact/maven/redirect?r=%s&g=%s&a=%s&v=%s&p=%s'
    resolved=version
    if "nexus.prod.cloud.fedex.com" in svr:
        # switch to rest v3 : https://help.sonatype.com/repomanager3/rest-and-integration-api/search-api#SearchAPI-SearchandDownloadAsset
        if "-SNAPSHOT" in version:
            META_DATA='%s/nexus/content/repositories/%s/%s/%s/%s/maven-metadata.xml' % (svr,repo, groupId.replace('.','/'), artifactId,version)
            if packaging is None:
                packaging = 'jar'
            resolved=download_metadata_version(META_DATA,packaging, version)
            install.LOGGER.info("version=%s resolved to %s" % (version,resolved))
        URL_PATTERN='%s/nexus/repository/%s/%s/%s/%s/%s-%s.%s' %  (svr, repo, groupId.replace('.','/'), artifactId, install.elvis(version , 'LATEST'), artifactId, install.elvis(resolved, version), install.elvis(packaging, 'zip'))
    else:
        URL_PATTERN= URL_PATTERN % (svr, repo, groupId, artifactId, install.elvis(version , 'LATEST'), install.elvis(packaging, 'zip'))

    outputFile = install.elvis(outputFile, "%s.%s" % (install.elvis(artifactId, 'ARTIFACT'),install.elvis(packaging, 'zip')))

    #install.LOGGER.info(---> download source %s" % (url)
    outputFile = __download(URL_PATTERN, gavstring)
    return outputFile

def subject_pull(fd,gav_giturl):
    fd_release_tag='master'
    __fd_project_url = gav_giturl.split('/')
    fd_project_dir = "config.%s" % __fd_project_url[len(__fd_project_url)-1].replace('.git','')
    install.LOGGER.info("checkout to %s" % (fd_project_dir))
    if ('->' in fd_project_dir):
      fd_release_tag = fd_project_dir.split('->')[1].strip()
      fd_project_dir = fd_project_dir.split('->')[0].strip()
    if os.path.exists(fd_project_dir):
        os.system("rm -fr %s" % (fd_project_dir))
    proc = os.system("set -xv && git clone -b %s %s %s" %(fd_release_tag, gav_giturl,fd_project_dir))

def subject_download(fd, gavString):
    '''com.fedex.sefs.common:sefs_silverControl'''
    if "git@" in gavString:
        install.LOGGER.info("pull deployment data from %s" % (gavString))
        subject_pull(fd,gavString)
    else:
        subject_nexus(fd,gavString)

def subject_nexus(fd, gavString):
    '''com.fedex.sefs.common:sefs_silverControl'''
    tarr = "%s" % (gavString)
    arr = tarr.strip().split(':')
    arr.reverse()
    # get first elelment popped
    project = arr.pop()
    arr.reverse()
    #install.LOGGER.info("arr=%s, fd=%s" % (arr,fd))
    fdrepo = fd['FD_REPO']
    fd['FD_SVR']="http"
    if '-SNAPSHOT' in gavString and fdrepo == 'releases':
        fdrepo = 'snapshots'
    # if "nexus.prod.cloud.fedex.com" in fd['FD_SVR'] and 'SEFS-6270' not in fdrepo:
    #     fdrepo = 'SEFS-6270-%s' % (fdrepo)
    #install.LOGGER.info("%s -> %s" % (project, str(arr)))
    fid = download(fd['FD_SVR'], fdrepo, arr)
    if fid:
        return {"config.%s" % (project): fid }
    else:
        return None


def unzip(fid):
    install.LOGGER.info("----> unpack: %s -> %s" % (fid.keys()[0], fid[fid.keys()[0]]))
    zip_ref = zipfile.ZipFile(fid[fid.keys()[0]], 'r')
    if fid.keys()[0] == '.':
        zip_ref.extractall()
    else:
        if os.path.exists(fid.keys()[0]) == True:
            shutil.rmtree(fid.keys()[0])
        zip_ref.extractall()
        if os.path.exists('config') == True:
            shutil.move('config', fid.keys()[0])
    zip_ref.close()


def main(argv=None):
    os.environ['https_proxy'] = 'https://internet.proxy.fedex.com:3128'
    os.environ['http_proxy'] = 'http://internet.proxy.fedex.com:3128'
    logging.basicConfig(level=logging.INFO)

    if argv is None:
        argv = sys.argv
    #install.LOGGER.info("args=%s" % (str(argv)))
    try:
        opts, args = getopt.getopt(sys.argv[1:], "hnv:s:d")
    except getopt.error, msg:
        print "Usage: install.py [-v version] [-h]\n%s\n-v [version]  - version of fdeploy (default=LATEST)\n-d   - debugging\n-h|--help      - this text" % (
            msg)
        print >>sys.stderr, msg
        print >>sys.stderr, "for help use --help"
        return 2
    fd = {}
    filesToUnpack = []
    elvis('FD_SOURCE', 'nexus', fd)
    elvis('FD_SVR', 'http://sefsmvn.ute.fedex.com:9999', fd)
    elvis('FD_USER', 'sefsdev', fd)
    elvis('FD_PASS', 'sefsdev', fd)
    elvis('FD_REPO', 'releases', fd)
    elvis('FD_MODULE', './fdeploy/src/main/resources', fd)
    elvis('PACKAGING', 'zip', fd)
    # process options
    fdeploy_version = "LATEST"
    fdeploy_repo = 'snapshots'
    fdeploy_base_install = True
    #install.LOGGER.info("FD=${fd}")
    for o, a in opts:
        if o in ("-h", "--help"):
            print __doc__
            sys.exit(0)
        elif o in ("-s"):
            fdeploy_repo = 'snapshots'
        elif o in ("-n", "--no-base"):
            fdeploy_base_install = False
        elif o in ("-d", "--debug"):
            logging.basicConfig(level=logging.DEBUG)
        elif o in ("-v", "--version"):
            fdeploy_version = a
            if '-SNAPSHOT' not in fdeploy_version:
                fdeploy_repo = 'releases'
    if fdeploy_base_install == True:
        FDEPLOY = "com.fedex.sefs.common:sefs_silverControl:%s:zip" % (fdeploy_version)
        if fdeploy_version.startswith("7."):
            FDEPLOY = "xopco.common.tools:xopco-common-tools-fdeploy:%s:zip" % (fdeploy_version)
            # fdeploy_repo = 'SEFS-6270-%s' % (fdeploy_repo)
            # elvis('FD_REPO', fdeploy_repo , fd)
            elvis('FD_SVR', 'https://nexus.prod.cloud.fedex.com:8443', fd)
        gav_fdeploy = FDEPLOY.split(':')
        install.LOGGER.info("retrieved fdeploy version: %s -> %s" % (
            gav_fdeploy, fdeploy_repo))
        unzip(
            {'.': download("%s" % (fd['FD_SVR']), fdeploy_repo, gav_fdeploy)})
    for gav_download_artifact in args:
        try:
            fid = subject_download(fd, gav_download_artifact)
            if fid:
                unzip(fid)
        except:
            pass


if __name__ == "__main__":
    sys.exit(main())
