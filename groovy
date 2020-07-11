job("task6job1") {
  description("This job will pull the github repo on every push, update the container using given Dockerfile and push image to DockerHub")
  
  scm {
    github('pasupuleti16/dynamic','master')
  }
  
  triggers {
    githubPush()
  }
  
  wrappers {
    preBuildCleanup()
  }
  
  steps {
    dockerBuildAndPublish {
      repositoryName('pasapuleti/httpd_container')
      tag("latest")
      dockerHostURI('tcp://0.0.0.0:4243')
      registryCredentials('docker')
      createFingerprints(false)
      skipDecorate(false)
      skipTagAsLatest(true)
    }
  }
  
}

job("task6job2") {
  description("This will run on slave nodes and control K8S.")
  triggers {
    upstream('task6job1', 'SUCCESS')
  }
  
  command = """
export len1=\$(ls -l /var/lib/jenkins/workspace/gvjob1 | grep html | wc -l)
if [ \$len1 -gt 0 ]
then
	export len2=\$(sudo kubectl get deployments | grep webserver | wc -l)
	if [ \$len2 -gt 0 ]
	then
		sudo kubectl rollout restart deployment/webserver
		sudo kubectl rollout status deployment/webserver
  else
		sudo kubectl create deployment webserver --image=pasapuleti/httpd_container:latest
		sudo kubectl scale deployment webserver --replicas=3
		sudo kubectl expose deployment webserver --port 80 --type NodePort
	fi
fi
"""
  
  steps {
    shell(command)
  }
  
}

job("task6job3") {
  description ("It will test if pod is running else send a mail")
  
  triggers {
    upstream('task6job2', 'SUCCESS')
  }
  steps {
    shell('''if sudo kubectl get deployments webserver
then
echo "send to production"
else
echo "sending back to developer"
exit 1
fi''')
  }
  publishers {
    extendedEmail {
      contentType('text/html')
      triggers {
        success{
          attachBuildLog(true)
          subject('Build successfull')
          content('The build was successful and deployment was done.')
          recipientList('p.rajeshbabu6666@gmail.com')
        }
        failure{
          attachBuildLog(true)
          subject('Failed build')
          content('The build was failed')
          recipientList('p.rajeshbabu6666@gmail.com')
        }
      }
    }
  }
}







buildPipelineView('task6') {
  filterBuildQueue(true)
  filterExecutors(false)
  title('task6')
  displayedBuilds(1)
  selectedJob('task6job1')
  alwaysAllowManualTrigger(false)
  showPipelineParameters(true)
  refreshFrequency(1)
}
