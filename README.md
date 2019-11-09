<div align="center">
  <img src="https://github.com/HS-Optimization-with-AI/Apex/blob/master/n_apex/src/Apex-Utility/APEX%20logo.png" height="500"><br><br>
</div>

# Apex
Recently Edge Computing paradigm has gained significant popularity both in industry and academia. With its increased usage in real-life scenarios, security, privacy and integrity of data in such environments have become critical. Malicious deletion of mission-critical data due to ransomware, trojans and viruses has been a huge menace and recovering such lost data is an active field of research. As most of Edge computing devices have compute and storage limitations, difficult constraints arise in providing an optimal scheme for data protection. These devices mostly use Linux/Unix based operating systems. Hence, this work focuses on extending the Ext4 file system to **APEX** (Adaptive Ext4): a file system based on novel on-the-fly learning model that provides an Adaptive Recover-ability Aware file allocation platform for efficient post-deletion data recovery and therefore maintaining data integrity. Our recovery model and its lightweight implementation allow significant improvement in recover-ability of lost data with lower compute, space, time, and cost overheads compared to other methods. We demonstrate the effectiveness of APEX through a case study of overwriting surveillance videos by CryPy malware on Raspberry-Pi based Edge deployment and show 678\% and 32\% higher recovery than Ext4 and current state-of-the-art File Systems. We also evaluate the overhead characteristics and experimentally show that they are lower than other related works.

## Related Works

## Class Diagram
![Alt text](https://github.com/HS-Optimization-with-AI/Apex/blob/master/images/uml.png?raw=true)

## Recovery Performance

## File I/O Performane

## CPU/RAM Overhead comparison

## References

* **Shreshth Tuli, Shikhar Tuli, Udit Jain, Rajkumar Buyya, [APEX: Adaptive Ext4 File System for Enhanced Data Recoverability in Edge Devices](https://arxiv.org/pdf/1910.01642.pdf), Proceedings of the 11th IEEE International Conference on Cloud Computing Technology and Science (CloudCom 2019, IEEE CS Press, USA), Sydney, Australia, December 11-13, 2019.**
* Shreshth Tuli, Redowan Mahmud, Shikhar Tuli, and Rajkumar Buyya, [FogBus: A Blockchain-based Lightweight Framework for Edge and Fog Computing.](http://buyya.com/papers/FogBus-JSS.pdf) Journal of Systems and Software (JSS), Volume 154, Pages: 22-36, ISSN: 0164-1212, Elsevier Press, Amsterdam, The Netherlands, August 2019. 
