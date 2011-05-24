# -*- coding: utf-8 -*-
import sys
from os import linesep
import subprocess
infname = sys.argv[1]
speed = sys.argv[2]

print infname, speed
inf = open (infname)
## UTILS ##

def dot(inputfile, format):
    '''graphviz dot
    inputfile a file name ending to .dot
    format: something like png, jpg, ps, svg, ...
    '''
    subprocess.call(['dot', '-T'+format, inputfile, '-o', inputfile[:-4]+'.'+format])
    

def rename(infname, namesfilestr, tre=infname):
    print infname
    import os.path
    if (os.path.isfile(infname + '.dot')):
        inf = open (infname + '.dot')
    else:
        inf = open (infname)
        infname = infname[:-4]
    outf = open(tre+'.o.dot','w')
    names = open(namesfilestr);
    name_map = {}
    for line in names:
        sp = line.split();
        name_map[sp[0]] = sp[1]
        print line
        
    print "jjjjj"
    for line in inf:
        print line
        for k,v in name_map.iteritems():
            #print line[:-1], '*',k, '"', line[len(k)+1], k + " "
            if len(line) > len(k) and line[:len(k)+1] == k + " ":
                line = line[:-3]
                line = line + ' label="{0}"];'.format(v)
                
        outf.write(line + "\n")
    inf.close()
    outf.close()
    names.close();
    # convert the output to png using dot command
    import subprocess
    #7.2 create the output picture
    subprocess.call(['dot', '-Tps', tre+'.o.dot', '-o', tre+'.ps'])
    subprocess.call(['dot', '-Tpng', tre+'.o.dot', '-o',   tre+'.png'])
    subprocess.call(['dot', '-Tsvg', tre+'.o.dot', '-o', tre+'.svg'])






#############################################################################











#0. determine the type of input file: sequence or triplet
firstline = inf.readline()
inf.close()
if firstline[0] == '>':
    input_type = 'sequence'
else:
    input_type= 'triplet'

if input_type == 'sequence':
    inf = open (infname)
    if infname[-4:] == '.txt':
        infname = infname [:-4] + '-'

    clustalinp = open (infname + 'clustalinp.txt', 'w')
    namesfile = open (infname + '.names' , 'w')

    names={}
    count=0
    ll=''
    #1. prepare the clustallw2 input file (fastA format)
    for line in inf:
        if line[0]=='>':
            names[line[1:]]=count
            if len(ll)>0:
                ll=ll.replace('\n','')
                ll=ll.replace('\r','')
                clustalinp.write(ll+linesep)
            ll=''
            clustalinp.write('>$'+str(count)+linesep)
            namesfile.write(str(count+1) + ' ' + line[1:-1] + linesep)
            count += 1
        else:
		    ll = ll + line
    if len(ll)>0:
        ll=ll.replace('\n','')
        ll=ll.replace('\r','')
        clustalinp.write(ll+linesep)
    clustalinp.close()
    inf.close()
    namesfile.close()

    #2. run the clustalw2 on input and align the sequences
    import subprocess
    subprocess.call(['ext/clustalw2 -infile="'+infname + 'clustalinp.txt"' + ' -outfile="'+infname + 'clustalout.txt"'],shell=True)

    #3. read the output of clustalw2
    clustaloutf = open ( infname + 'clustalout.txt' )
    clustaloutf.readline()
    clustaloutf.readline()
    clustaloutf.readline()
    alignedseqs = {}
    for i in range(count):
        alignedseqs[i]=''
        
    #3.1. clustal breaks the sequences in multiple blocks so they should be joined together
    for line in clustaloutf:
        if line[0]=='$':
            n = int(line[1:line.find(' ')])
            line=(line[line.find(' '):]).strip()
            alignedseqs[n] += line

    for tt in alignedseqs.values():
        print tt


    mulignedlen = len(alignedseqs[0])

        
    #4. prepare an input file for phyml, run it on each triplet and write the output triplet file
    l = len(alignedseqs[0])
    def consensus(*sequences):
        ret = ''
        for i in range(l):
            charcount={}
            for s in sequences:
                if not s[i] in charcount:
                    charcount[s[i]]=0
                charcount[s[i]]+=1
            maxc,maxn = '',0
            for c,n in charcount.iteritems():
                if n>maxn:
                    maxc,maxn = c,n
            if maxc!='-':
                ret += maxc
        return ret
            
    def isDNA(seq):
        seq=seq.upper()
        for c in seq:
            if c!='A' and c!='G' and c!='C' and c!='T' and c!='-':
                return False
        return True
    _ = alignedseqs
    if isDNA(_[0]):
        t=0
    else:
        t=1

    def getTriplet(split):
        "split is a phyml tree format string"
        s = split
        print s
        t = lambda x:s.find('$',x)
        u = lambda x:s.find(':',t(x))
        ss = lambda x: int(s[t(x)+1:u(x)])
        ip = t(0)
        jp = t(ip+1)
        kp = t(jp+1)
        zp = t(kp+1)
        i,j,k,z = ss(ip),ss(jp), ss(kp), ss(zp)
        tt23 = s.find(':',jp+1)
        w=float(s[s.find(':',tt23+1)+1: kp-1])
        l = s.find(')')
        w=float(s[l+1: s.find(':',l)-1])
        #print w
        if w==0:
            return ''
        else:
            tr = lambda x,y,z: "{0} {1} {2}".format(x,y,z)
            if i==0:
                return tr(k,z,j)
            elif j==0:
                return tr(k,z,i)
            elif k==0:
                return tr(i,j,z)
            elif z==0:
                return tr(i,j,k)

    ttt = lambda i,j,k: str(i+1) + ' ' + str(j+1) + ' ' + str(k+1)
    outf = open (infname + 'triplets.txt','w')

    c = consensus(*alignedseqs.values())
    for i in range(count):
        for j in range(i):
            for k in range(j):
                phymlinp = open (infname+ 'phymlinp.txt', 'w')
                phymlinp.write('4 ' + str(l) + linesep)
                phymlinp.write('$0 ' + c                + linesep)
                phymlinp.write('$'+str(i+1) + ' ' + _[i]+ linesep)
                phymlinp.write('$'+str(j+1) + ' ' + _[j]+ linesep)
                phymlinp.write('$'+str(k+1) + ' ' + _[k]+ linesep)
                phymlinp.close()
    #            print i,j,k
    #            print "./phyml {0} {1} i 2 0 HKY 4.0 e 1 1.0 BIONJ y n".format(infname + 'phymlinp.txt',t)
                subprocess.call(["ext/phyml -i '{0}' > a.txt".format(infname +'phymlinp.txt',t)],shell=True)
                #4.2. now read and parse phyml output
                phymlout = open (infname + 'phymlinp.txt_phyml_tree.txt')
                triplet= getTriplet(phymlout.readline())
                if triplet != '':
                    print triplet
                    outf.write(triplet + linesep)

    outf.close()           


    #5. removing unnecessary files
    print 'removing unnecessary files'
    import os
    os.remove(infname + 'clustalinp.txt')
    os.remove(infname + 'clustalinp.dnd')
    os.remove(infname + 'clustalout.txt')
    os.remove(infname+ 'phymlinp.txt')
    os.remove(infname + 'phymlinp.txt_phyml_tree.txt')
    os.remove(infname + 'phymlinp.txt_phyml_stats.txt')
    print infname + 'triplets.txt generated successfully! :)'
    print "...."
    print "And now going to visit the last level's GHOUL!!!"
    print "%%%%"

    print "Running TripNet Algorithm on triplets..."

    #6. run TripNet on generated triplets
    subprocess.call(['java -cp "lib/*:bin" phylogenetic.TripNet "{0}triplets.txt" -v -speed {1}'.format(infname, speed)], shell=True)

    print "The network has been built successfully"
    print "Now rename the leaves of network to sequence names..."
    tre=infname[:-1]
    #7. rename the generated .dot file and convert it to image


    rename(infname + 'triplets.txt', infname + '.names')

    os.remove(infname + '.names')
    os.remove(infname + 'triplets.txt.dot')

else:   #the input file is a triplet file
    import subprocess
    subprocess.call(['java -cp "lib/*:bin" phylogenetic.TripNet "{0}" -v -speed {1}'.format(infname, speed)], shell=True)
    #os.exit()
    import os
    if os.path.exists(infname+'.names'):
        rename(infname, infname+'.names', infname)
    else:
        dot(infname+'.dot','png')
    print 'finished'
