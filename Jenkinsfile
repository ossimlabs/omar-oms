properties([
    parameters ([
        booleanParam(name: 'CLEAN_WORKSPACE', defaultValue: true, description: 'Clean the workspace at the end of the run'),
        string(name: 'DOCKER_REGISTRY_DOWNLOAD_URL', defaultValue: 'nexus-docker-private-group.ossim.io', description: 'Repository of docker images')
    ]),
    pipelineTriggers([
            [$class: "GitHubPushTrigger"]
    ]),
    [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/ossimlabs/omar-oms'],
    buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '3', daysToKeepStr: '', numToKeepStr: '20')),
    disableConcurrentBuilds()
])
podTemplate(
  containers: [
    containerTemplate(
      name: 'docker',
      image: 'docker:19.03.11',
      ttyEnabled: true,
      command: 'cat',
      privileged: true
    ),
    containerTemplate(
      image: "${DOCKER_REGISTRY_DOWNLOAD_URL}/omar-builder:latest",
      name: 'builder',
      command: 'cat',
      ttyEnabled: true
    ),
    containerTemplate(
      image: "${DOCKER_REGISTRY_DOWNLOAD_URL}/alpine/helm:3.2.3",
      name: 'helm',
      command: 'cat',
      ttyEnabled: true
    ),
    containerTemplate(
      image: "${DOCKER_REGISTRY_DOWNLOAD_URL}/kubectl-aws-helm:latest",
      name: 'kubectl-aws-helm',
      command: 'cat',
      ttyEnabled: true,
      alwaysPullImage: true
    ),
    containerTemplate(
      name: 'cypress',
      image: "${DOCKER_REGISTRY_DOWNLOAD_URL}/cypress/included:4.9.0",
      ttyEnabled: true,
      command: 'cat',
      privileged: true
    )
  ],
  volumes: [
    hostPathVolume(
      hostPath: '/var/run/docker.sock',
      mountPath: '/var/run/docker.sock'
    ),
  ]
)
{
  node(POD_LABEL){

      stage("Checkout branch")
      {
        
        scmVars = checkout(scm)
    
        GIT_BRANCH_NAME = scmVars.GIT_BRANCH
        BRANCH_NAME = """${sh(returnStdout: true, script: "echo ${GIT_BRANCH_NAME} | awk -F'/' '{print \$2}'").trim()}"""
        sh """
        touch buildVersion.txt
        grep buildVersion gradle.properties | cut -d "=" -f2 > "buildVersion.txt"
        """
        preVERSION = readFile "buildVersion.txt"
        VERSION = preVERSION.substring(0, preVERSION.indexOf('\n'))

        GIT_TAG_NAME = "omar-oms" + "-" + VERSION
        ARTIFACT_NAME = "ArtifactName"

        script {
          if (BRANCH_NAME != 'master') {
            buildName "${VERSION} - ${BRANCH_NAME}-SNAPSHOT"
          } else {
            buildName "${VERSION} - ${BRANCH_NAME}"
          }
        }
      }

      stage("Load Variables")
      {
        withCredentials([string(credentialsId: 'o2-artifact-project', variable: 'o2ArtifactProject')]) {
          step ([$class: "CopyArtifact",
            projectName: o2ArtifactProject,
            filter: "common-variables.groovy",
            flatten: true])
          }
          load "common-variables.groovy"
          
        switch (BRANCH_NAME) {
        case "master":
          TAG_NAME = VERSION
          break

        case "dev":
          TAG_NAME = "latest"
          break

        default:
          TAG_NAME = BRANCH_NAME
          break
      }

    DOCKER_IMAGE_PATH = "${DOCKER_REGISTRY_PRIVATE_UPLOAD_URL}/omar-oms"
    
      }

      stage('SonarQube Analysis') {
          nodejs(nodeJSInstallationName: "${NODEJS_VERSION}") {
              def scannerHome = tool "${SONARQUBE_SCANNER_VERSION}"

              withSonarQubeEnv('sonarqube'){
                  sh """
                    ${scannerHome}/bin/sonar-scanner \
                    -Dsonar.projectKey=omar-oms \
                    -Dsonar.login=${SONARQUBE_TOKEN}
                  """
              }
          }
      }
        // stage ("Generate Swagger Spec") {
        //   container('builder') {
        //         sh """
        //         ./gradlew :omar-oms-plugin:generateSwaggerDocs \
        //             -PossimMavenProxy=${MAVEN_DOWNLOAD_URL}
        //         """
        //         archiveArtifacts "plugins/*/build/swaggerSpec.json"
        //     }
        //   }
        // stage ("Run Cypress Test") {
        //     container('cypress') {
        //         try {
        //             sh """
        //             cypress run --headless
        //             """
        //         } catch (err) {
        //             sh """
        //             npm i -g xunit-viewer
        //             xunit-viewer -r results -o results/omar-oms-test-results.html
        //             """
        //             junit 'results/*.xml'
        //             archiveArtifacts "results/*.xml"
        //             archiveArtifacts "results/*.html"
        //             s3Upload(file:'results/omar-oms-test-results.html', bucket:'ossimlabs', path:'cypressTests/')
        //         }
        //     }
        // }

      stage('Build') {
        container('builder') {
          sh """
          ./gradlew assemble \
              -PossimMavenProxy=${MAVEN_DOWNLOAD_URL}
          ./gradlew copyJarToDockerDir \
              -PossimMavenProxy=${MAVEN_DOWNLOAD_URL}
          """
          archiveArtifacts "plugins/*/build/libs/*.jar"
          archiveArtifacts "apps/*/build/libs/*.jar"
        }
      }

    stage ("Publish Nexus"){
      container('builder'){
          withCredentials([[$class: 'UsernamePasswordMultiBinding',
                          credentialsId: 'nexusCredentials',
                          usernameVariable: 'MAVEN_REPO_USERNAME',
                          passwordVariable: 'MAVEN_REPO_PASSWORD']])
          {
            sh """
            ./gradlew publish \
                -PossimMavenProxy=${MAVEN_DOWNLOAD_URL}
            """
          }
        }
    }
 //testingWebHook
    stage('Docker build') {
      container('docker') {
        withDockerRegistry(credentialsId: 'dockerCredentials', url: "https://${DOCKER_REGISTRY_DOWNLOAD_URL}") {  //TODO
          if (BRANCH_NAME == 'master'){
                sh """
                    docker build --build-arg BASE_IMAGE=${DOCKER_REGISTRY_DOWNLOAD_URL}/ossim-alpine-runtime:dev --network=host -t "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-oms:"${VERSION}" ./docker
                """
          }
          else {
                sh """
                    docker build --build-arg BASE_IMAGE=${DOCKER_REGISTRY_DOWNLOAD_URL}/ossim-alpine-runtime:dev--network=host -t "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-oms:"${VERSION}".a ./docker
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
                    docker push "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-oms:"${VERSION}"
                """
            }
            else if (BRANCH_NAME == 'dev') {
                sh """
                    docker tag "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-oms:"${VERSION}".a "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-oms:dev
                    docker push "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-oms:"${VERSION}".a
                    docker push "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-oms:dev
                """
            }
            else {
                sh """
                    docker push "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-oms:"${VERSION}".a           
                """
            }
          }
        }
      }
    stage('Package chart'){
      container('helm') {
        sh """
            mkdir packaged-chart
            helm package -d packaged-chart chart
          """
      }
    }
    stage('Upload chart'){
      container('builder') {
        withCredentials([usernameColonPassword(credentialsId: 'helmCredentials', variable: 'HELM_CREDENTIALS')]) {
          sh "curl -u ${HELM_CREDENTIALS} ${HELM_UPLOAD_URL} --upload-file packaged-chart/*.tgz -v"
        }
      }
    }
      
    stage('New Deploy'){
        container('kubectl-aws-helm') {
            withAWS(
            credentials: 'Jenkins IAM User',
            region: 'us-east-1'){
                if (BRANCH_NAME == 'master'){
                    //insert future instructions here
                }
                else if (BRANCH_NAME == 'dev') {
                    sh "aws eks --region us-east-1 update-kubeconfig --name gsp-dev-v2 --alias dev"
                    sh "kubectl config set-context dev --namespace=omar-dev"
                    sh "kubectl rollout restart deployment/omar-oms"   
                }
                else {
                    sh "echo Not deploying ${BRANCH_NAME} branch"   
                }
            }
        }
    }  
      
    stage("Clean Workspace"){
      if ("${CLEAN_WORKSPACE}" == "true")
        step([$class: 'WsCleanup'])
    }
  }
}
