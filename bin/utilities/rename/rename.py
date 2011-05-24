
def rename(infname, namesfilestr):
    inf = open (infname + '.dot')
    outf = open(infname+'.zrename.dot','w')
    names = open(namesfilestr);
    name_map = {}
    for line in names:
        sp = line.split();
        name_map[sp[0]] = sp[1]
        print line

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
    subprocess.call(['dot', '-Tpng', infname+'.zrename.dot', '-O'+ infname+'.zrename.dot.png'])









import os
os.chdir('data')
namesfilestr = ''
for f in os.listdir('.'):
    if f[-5:] == 'names':
        namesfilestr = f

if namesfilestr!='':
    for f in os.listdir('.'):
        if f[-3:] == 'dot' and f[-11:] != 'zrename.dot':
            rename(f[:-4], namesfilestr)
    

