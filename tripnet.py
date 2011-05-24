#!/usr/bin/env python

import os
import subprocess
import sys 

if len(sys.argv) == 1:
    print '''
Usage: python tripnet.py file_name [slow | normal | fast]

- file_name is the path of input file or directory, there is some options for this:
    1- If file_name is the name of a file containing sequences first some triplet will be 
        created based on sequences and then the network for this triplets will be created.
        * In this case the file format should be in FASTA format like this:
                mysequences.txt:
                
                >sse94 
                ggtgcgcgagggcggccgcccgataagcggcgacaccggtctgcgcga
                >und8 
                ggtacgcgagggcgcccgcccgataagcggcgacaccggtctgcgcga  
                >und64 
                ggtgcgcgagggcgtccgcccgataagcggcgacaccggtctgcgcga
                >she49_1 
                ggggcgcgagggcgcccgcacgataagcggcgacaccggtctgcgcga
                >und79 
                ggtgcgcgagggcgaccgcccgataagcggcgacaccggtctgcgcga
                >Smb_17 
                ggtgcgcgagggcgcccgcccgataagcggcgacaccggtctgcgcga
        
    2- If file_name refers to a triplets file, the network will be created directly from
         these triplets.
        * In this case your input file should have one triplet in each line, and the 
          numbers in the file should be between 0 and n, like this:
                mytriplets.txt
                
                4 5 1
                4 5 2
                4 5 3
                3 6 1
                3 6 2
                6 3 4
                6 3 5
                5 4 6 
        * You can use a .names file 
    3- If file_name is the name of a directory, each file in the directory will be processed
         individually as described above.

- The second input is the speed of algorithm, in a higher speed you will obtain a network
with higher level, but choosing a slow speed will provide you a better network but it may 
take a long time for the algorithm to finish.

examples: 
Running TripNet on a triplet file
>> python tripnet.py mytriplets.txt normal

Running TripNet on all files of a directory
>> python tripnet.py dir fast

please refer to the user manual for more information.
'''

else:
    inf = sys.argv[1]
    if len(sys.argv)> 2:
        speed = sys.argv[2]
    else:
        speed = 'slow'
        
    if os.path.isdir(inf):
        if inf[-1:]=='\\' or inf[-1:]=='/':
            inf = inf[:-1]
        l = os.listdir(inf)
        for filename in l:
            subprocess.call(['python', 'seq.py', inf+'/'+filename, speed])
    else:
        subprocess.call(['python', 'seq.py', inf, speed])
