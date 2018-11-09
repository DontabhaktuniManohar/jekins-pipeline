# -*- coding: utf-8 -*-
import xml.etree.ElementTree as ET
import getopt
try:  # pragma no cover
    _unicode = unicode
except NameError:  # pragma no cover
    _unicode = str
import sys
import os

os.environ['HTTPS_PROXY'] = "http://internet.proxy.fedex.com:3128"
os.environ['HTTP_PROXY'] = "http://internet.proxy.fedex.com:3128"

def parse_jacoco_exclusion(xml_input, encoding=None):
    if isinstance(xml_input, _unicode):
        if not encoding:
            encoding = 'utf-8'
        xml_input = xml_input.encode(encoding)
    root = ET.fromstring(xml_input)
    a = ".//{" + root.tag[root.tag.find("{")+1:root.tag.find("}")]  + "}exclude"
    check = []
    for child in root.findall("%s" % a):
       check.append(child.text)
    return ','.join(check).strip()


def main(argv=None):
    if argv is None:
        argv = sys.argv
    try:
        opts, args = getopt.getopt(argv[1:], "hf:j")
    except getopt.error, msg:
        print "Usage: cicd_tools.py [-j][-f pom.xml] [-h]\n%s\n\n-h|--help      - this text" % (msg)
        print >>sys.stderr, msg
        print >>sys.stderr, "for help use --help"
        return 2
    pomFile = os.path.abspath('pom.xml')
    jacoco_exclusion = True
    for o, a in opts:
        if o in ("-h", "--help"):
            print __doc__
            sys.exit(0)
        elif o in ("-f"):
            pomFile = os.path.abspath(a)
        elif o in ("-j"):
            jacoco_exclusion = True
    if os.path.exists(pomFile):
        with open(pomFile, "r") as f:
            xml_input = f.readlines()
        content = '\n'.join([x.strip() for x in xml_input])
        if jacoco_exclusion == True:
            parse_jacoco_exclusion(content)
    else:
        raise Exception("file '%s' does not exist." % (pomFile))



if __name__ == '__main__':
    main(sys.argv)
