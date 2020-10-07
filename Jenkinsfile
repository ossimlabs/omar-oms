    stage('Docker build') {
      container('docker') {
        withDockerRegistry(credentialsId: 'dockerCredentials', url: "https://${DOCKER_REGISTRY_DOWNLOAD_URL}") {  //TODO
          if (BRANCH_NAME == 'master'){
                sh """
                    docker build --network=host -t "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-wms-app:"${VERSION}" ./docker
                """
          }
          else {
                sh """
                    docker build --network=host -t "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-wms-app:"${VERSION}".a ./docker
                """
          }
        }
      }
    }
	
	    stage('Docker push'){
        container('docker') {
          withDockerRegistry(credentialsId: 'dockerCredentials', url: "https://${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}") {
            if (BRANCH_NAME == 'master'){
                sh """
                    docker push "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-wms-app:"${VERSION}"
                """
            }
            else if (BRANCH_NAME == 'dev') {
                sh """
                    docker tag "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-wms-app:"${VERSION}".a "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-wms-app:dev
                    docker push "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-wms-app:"${VERSION}".a
                    docker push "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-wms-app:dev
                """
            }
            else {
                sh """
                    docker push "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-wms-app:"${VERSION}".a           
                """
            }
          }
        }
      }
	
	
	    stage('Docker build') {
      container('docker') {
        withDockerRegistry(credentialsId: 'dockerCredentials', url: "https://${DOCKER_REGISTRY_DOWNLOAD_URL}") {  //TODO
          if (BRANCH_NAME == 'master'){
                sh """
                    docker build --build-arg BASE_IMAGE=${DOCKER_REGISTRY_DOWNLOAD_URL}/ossim-alpine-runtime:dev --network=host -t "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-oms-app:"${VERSION}" ./docker

                """
          }
          else {
                sh """
                    docker build --build-arg BASE_IMAGE=${DOCKER_REGISTRY_DOWNLOAD_URL}/ossim-alpine-runtime:dev --network=host -t "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-wms-app:"${VERSION}".a ./docker

                """
          }
        }
      }
    }
	
	    stage('Docker push'){
        container('docker') {
          withDockerRegistry(credentialsId: 'dockerCredentials', url: "https://${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}") {
            if (BRANCH_NAME == 'master'){
                sh """
                    docker push "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-wms-app:"${VERSION}"
                """
            }
            else if (BRANCH_NAME == 'dev') {
                sh """
                    docker tag "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-wms-app:"${VERSION}".a "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-wms-app:dev
                    docker push "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-wms-app:"${VERSION}".a
                    docker push "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-wms-app:dev
                """
            }
            else {
                sh """
                    docker push "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-wms-app:"${VERSION}".a           
                """
            }
          }
        }
      }
	
	

