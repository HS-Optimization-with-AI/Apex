<div align="center">
  <img src="https://github.com/HS-Optimization-with-AI/Apex/blob/master/n_apex/src/Apex-Utility/APEX%20logo.png" height="400"><br><br>
</div>

# Apex
Recently Edge Computing paradigm has gained significant popularity both in industry and academia. With its increased usage in real-life scenarios, security, privacy and integrity of data in such environments have become critical. Malicious deletion of mission-critical data due to ransomware, trojans and viruses has been a huge menace and recovering such lost data is an active field of research. As most of Edge computing devices have compute and storage limitations, difficult constraints arise in providing an optimal scheme for data protection. These devices mostly use Linux/Unix based operating systems. Hence, this work focuses on extending the Ext4 file system to **APEX** (Adaptive Ext4): a file system based on novel on-the-fly learning model that provides an Adaptive Recover-ability Aware file allocation platform for efficient post-deletion data recovery and therefore maintaining data integrity. Our recovery model and its lightweight implementation allow significant improvement in recover-ability of lost data with lower compute, space, time, and cost overheads compared to other methods. We demonstrate the effectiveness of APEX through a case study of overwriting surveillance videos by CryPy malware on Raspberry-Pi based Edge deployment and show 678\% and 32\% higher recovery than Ext4 and current state-of-the-art File Systems. We also evaluate the overhead characteristics and experimentally show that they are lower than other related works.

## Comparison baselines

* Ext4: Base Ext4 file system
* AFS (Andrew File System): provides  a  backup  mechanism  torecover deleted or lost files for a limited period of time. This is not suitable for Fog nodes due to limited disk space available(there is high storage-to-compute cost ratio in Fog framework deployment  and  other  communication  limitations  across  Fog network). 
[AFS] J. H. Howard, Kazar, M. L., Menees, S. G., Nichols, D. A., Satya-narayanan, M., Sidebotham, R. N., and West, M. J. Scale and performance in a distributed file system. ACM Transactions on Computer Systems, 6(1):5181, February 1988
* FFS (Forensic File System): provides forensic fileidentifiers at cluster-level file allocation to provide information needed for file types to be recovered after deletion. As they are only limited to file cluster identification and the identification of file  types,  the amount of recover-ability of data is limited because they completely ignore the file usage characteristics and temporal locality. 
[FFS] M. Alhussein, A. Srinivasan and D. Wijesekera, “Forensics filesystemwith cluster-level identifiers for efficient data recovery,” 2012 International Conference for Internet Technology and Secured Transactions.
* ShieldFS: provides a self-healing ransomware-aware filesystem which is capable of both detection and the recovery from ransomware attacks. It works by analyzing the I/O data trace of various processes and uses aclassifier to detect if the process is maliciously deleting data. Ifsuch a process is discovered, it uses a recovery approach usedby copy-on-write filesystems.
[ShieldFS] A. Continella, A. Guagnelli, G. Zingaro, G. De Pasquale, A. Barenghi,. Zanero, and F. Maggi. “ShieldFS: a self-healing, ransomware-awarefilesystem,” Proceedings of the 32nd Conference on Computer SecurityApplications, 2016
* ExtSFR: uses  files’  metadata  to  identify  and  recover  them,  but ignores file usage and access characteristics which limit recoverability
[ExtSFR] S. Lee, W. Jo, S. Eo, and Taeshik Shon. “ExtSFR: scalable filerecovery framework based on an Ext file system.” Multimedia  Tools and Applications (2019): 1-19

## Class Diagram
<div align="center">
  <img src="https://github.com/HS-Optimization-with-AI/Apex/blob/master/images/uml.png" height="400"><br><br>
</div>

## Recovery Performance
![Alt text](https://github.com/HS-Optimization-with-AI/Apex/blob/master/images/recovery1.png?raw=true)
![Alt text](https://github.com/HS-Optimization-with-AI/Apex/blob/master/images/recovery2.png?raw=true)

## File I/O Performance
![Alt text](https://github.com/HS-Optimization-with-AI/Apex/blob/master/images/read.png?raw=true)
![Alt text](https://github.com/HS-Optimization-with-AI/Apex/blob/master/images/write.png?raw=true)
![Alt text](https://github.com/HS-Optimization-with-AI/Apex/blob/master/images/delete.png?raw=true)

## CPU/RAM Overhead comparison
![Alt text](https://github.com/HS-Optimization-with-AI/Apex/blob/master/images/cpu.png?raw=true)
![Alt text](https://github.com/HS-Optimization-with-AI/Apex/blob/master/images/ram.png?raw=true)

## References

* **Shreshth Tuli, Shikhar Tuli, Udit Jain, Rajkumar Buyya, [APEX: Adaptive Ext4 File System for Enhanced Data Recoverability in Edge Devices](https://arxiv.org/pdf/1910.01642.pdf), Proceedings of the 11th IEEE International Conference on Cloud Computing Technology and Science (CloudCom 2019, IEEE CS Press, USA), Sydney, Australia, December 11-13, 2019.**
* Shreshth Tuli, Redowan Mahmud, Shikhar Tuli, and Rajkumar Buyya, [FogBus: A Blockchain-based Lightweight Framework for Edge and Fog Computing.](http://buyya.com/papers/FogBus-JSS.pdf) Journal of Systems and Software (JSS), Volume 154, Pages: 22-36, ISSN: 0164-1212, Elsevier Press, Amsterdam, The Netherlands, August 2019. 
