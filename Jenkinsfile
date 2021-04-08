properties([
    parameters([
        string(name: 'PROJECT_URL', defaultValue: 'https://github.com/ossimlabs/omar-oms', description: 'The project github URL'),
        string(name: 'DOCKER_REGISTRY_DOWNLOAD_URL', defaultValue: 'nexus-docker-private-group.ossim.io', description: 'Repository of docker images')
    ]),
    pipelineTriggers([
        [$class: "GitHubPushTrigger"]
    ]),
    [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: '${PROJECT_URL}'],
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
            image: "${DOCKER_REGISTRY_DOWNLOAD_URL}/omar-builder:jdk11",
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
            name: 'git',
            image: 'alpine/git:latest',
            ttyEnabled: true,
            command: 'cat',
            envVars: [
                envVar(key: 'HOME', value: '/root')
                ]
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
    stage("Checkout branch") {
        APP_NAME = PROJECT_URL.tokenize('/').last()
        scmVars = checkout(scm)
        Date date = new Date()
        String currentDate = date.format("YYYY-MM-dd-HH-mm-ss")
        MASTER = "master"
        GIT_BRANCH_NAME = scmVars.GIT_BRANCH
        BRANCH_NAME = """${sh(returnStdout: true, script: "echo ${GIT_BRANCH_NAME} | awk -F'/' '{print \$2}'").trim()}"""
        VERSION = """${sh(returnStdout: true, script: "cat chart/Chart.yaml | grep version: | awk -F'version:' '{print \$2}'").trim()}"""
        GIT_TAG_NAME = APP_NAME + "-" + VERSION
        ARTIFACT_NAME = "ArtifactName"

            if (BRANCH_NAME == "${MASTER}") {
                buildName "${VERSION}"
                TAG_NAME = "${VERSION}"
            }
            else {
                buildName "${BRANCH_NAME}-${currentDate}"
                TAG_NAME = "${BRANCH_NAME}-${currentDate}"
            }

    }

    stage("Load Variables") {
        withCredentials([string(credentialsId: 'o2-artifact-project', variable: 'o2ArtifactProject')]) {
            step ([$class: "CopyArtifact",
                projectName: o2ArtifactProject,
                filter: "common-variables.groovy",
                flatten: true])
        }
        load "common-variables.groovy"
        DOCKER_IMAGE_PATH = "${DOCKER_REGISTRY_PRIVATE_UPLOAD_URL}/${APP_NAME}"
    }

    stage ("Run Cypress Test") {
        container('cypress') {
            try {
                sh """
                    cypress run --headless
                """
            }
            catch (err) {

            }
                sh """
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
             withCredentials([[$class: 'UsernamePasswordMultiBinding',
                          credentialsId: 'nexusCredentials',
                          usernameVariable: 'MAVEN_REPO_USERNAME',
                          passwordVariable: 'MAVEN_REPO_PASSWORD']])
            {
                sh """
                    ./gradlew assemble -PossimMavenProxy=${MAVEN_DOWNLOAD_URL} -PbranchName=${BRANCH_NAME}
                    ./gradlew copyJarToDockerDir -PossimMavenProxy=${MAVEN_DOWNLOAD_URL} -PbranchName=${BRANCH_NAME}
                    ./gradlew publish -PossimMavenProxy=${MAVEN_DOWNLOAD_URL} -PbranchName=${BRANCH_NAME}
                """
                archiveArtifacts "plugins/*/build/libs/*.jar"
                archiveArtifacts "apps/*/build/libs/*.jar"
            }
        }
    }

    stage('Docker Build') {
        container('docker') {
            withDockerRegistry(credentialsId: 'dockerCredentials', url: "https://${DOCKER_REGISTRY_DOWNLOAD_URL}") {
                sh """
                    docker build --build-arg BASE_IMAGE=${DOCKER_REGISTRY_DOWNLOAD_URL}/ossim-alpine-jdk11-runtime:1.3 --network=host -t "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}/${APP_NAME}:${TAG_NAME}" ./docker
                """
            }
        }
    }

    stage('Docker Push') {
        container('docker') {
            withDockerRegistry(credentialsId: 'dockerCredentials', url: "https://${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}") {
            sh """
                docker tag "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}/${APP_NAME}:${TAG_NAME}" "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}/${APP_NAME}:${TAG_NAME}"
                docker push "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}/${APP_NAME}:${TAG_NAME}"
            """
            }
        }
    }

    stage('Package & Upload Chart'){
        container('helm') {
            sh """
                mkdir packaged-chart
                helm package -d packaged-chart chart
            """
        withCredentials([usernameColonPassword(credentialsId: 'helmCredentials', variable: 'HELM_CREDENTIALS')]) {
            sh """
                apk add curl
                curl -u ${HELM_CREDENTIALS} ${HELM_UPLOAD_URL} --upload-file packaged-chart/*.tgz -v
            """
            }
        }
    }

    stage('Tag Repo') {
        when (BRANCH_NAME == MASTER) {
            container('git') {
                withCredentials([sshUserPrivateKey(
                      credentialsId: env.GIT_SSH_CREDENTIALS_ID,
                      keyFileVariable: 'SSH_KEY_FILE',
                      passphraseVariable: '',
                      usernameVariable: 'SSH_USERNAME')]) {
                          sh """
                              mkdir ~/.ssh
                              echo -e "StrictHostKeyChecking=no\nIdentityFile ${SSH_KEY_FILE}" >> ~/.ssh/config
                              git config user.email "radiantcibot@gmail.com"
                              git config user.name "Jenkins"
                              git tag -a "${GIT_TAG_NAME}" \
                              -m "Generated by: ${env.JENKINS_URL}" \
                              -m "Job: ${env.JOB_NAME}" \
                              -m "Build: ${env.BUILD_NUMBER}"
                              git push -v origin "${GIT_TAG_NAME}"
                          """
                        }
                    }
                }
            }
        }
    }