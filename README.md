# Java Application
This is a Simple Blogging Application written in Java Language to test DevSecOps Practices..

In ```dev``` branch, i have CI-CD jenkins pipeline to build image and deploy into Dev kubernetes cluster directly without validating using ```ArgoCD```  
In ```main``` branch, i have CD jenkins pipeline to update the latest Image Tag in github repo and deploy into prod kubernetes cluster using ```ArgoCD```  
In ```staging``` branch, i have CD jenkins pipeline to update the latest Image Tag in github repo and deploy into QA kubernetes cluster using ```ArgoCD```  
