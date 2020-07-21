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

      stage("Checkout branch $BRANCH_NAME")
      {
          checkout(scm)
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

        stage ("Generate Swagger Spec") {
          container('builder') {
                sh """
                ./gradlew :omar-oms-plugin:generateSwaggerDocs \
                    -PossimMavenProxy=${MAVEN_DOWNLOAD_URL}
                """
                archiveArtifacts "plugins/*/build/swaggerSpec.json"
            }
          }

        stage ("Run Cypress Test") {
            container('builder') {
                sh """
                npm install cypress
                apt-get install xvfb
                npx cypress run
                npm i -g xunit-viewer
                xunit-viewer -r results -o results/omar-oms-test-results.html
                """
                junit 'results/*.xml'
                archiveArtifacts "results/*.xml"
                archiveArtifacts "results/*.html"
                s3Upload(file:'results/omar-oms-test-results.html', bucket:'ossimlabs', path:'cypressTests/')
            }
        }

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
    stage('Docker build') {
      container('docker') {
        withDockerRegistry(credentialsId: 'dockerCredentials', url: "https://${DOCKER_REGISTRY_DOWNLOAD_URL}") {  //TODO
          sh """
            docker build --build-arg BASE_IMAGE=${DOCKER_REGISTRY_DOWNLOAD_URL}/ossim-alpine-runtime:dev --network=host -t "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-oms-app:${BRANCH_NAME} ./docker
          """
        }
      }
    }
    stage('Docker push'){
      container('docker') {
        withDockerRegistry(credentialsId: 'dockerCredentials', url: "https://${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}") {
        sh """
            docker push "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-oms-app:${BRANCH_NAME}
        """
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
    stage("Clean Workspace"){
      if ("${CLEAN_WORKSPACE}" == "true")
        step([$class: 'WsCleanup'])
    }
  }
}
