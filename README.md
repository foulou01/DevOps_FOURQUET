# DevOps_FOURQUET
1-1 For which reason is it better to run the container with a flag -e to give the environment variables rather than put them directly in the Dockerfile?

It is preferable to use the -e flag to pass environment variables when launching the container, as this avoids storing sensitive information in the Docker image, offers greater flexibility to change the configuration without rebuilding the image, and respects good practice in separating code and configuration.

1-2 Why do we need a volume to be attached to our postgres container?

To guarantee data persistence.
Without a volume, all the data in the database is lost if the container is deleted or recreated, as it only remains in the container's temporary file system.
By attaching a volume, the data is stored on the host's disk, allowing the database to be preserved even after the container is deleted or recreated.
This is essential for any database in production.

1-3 Document your database container essentials: commands and Dockerfile.

**Commandes essentielles :**

- **Construire l’image :**
  ```
  docker build -t mon-image-postgres .
  ```

- **Créer un réseau pour la communication entre conteneurs :**
  ```
  docker network create app-network
  ```

- **Lancer le conteneur PostgreSQL avec persistance des données :**
  ```
  docker run -d --name db --network app-network ^
    -e POSTGRES_DB=db -e POSTGRES_USER=usr -e POSTGRES_PASSWORD=pwd ^
    -v "C:\Users\louis\OneDrive - Fondation EPF\DATA\DevOps\TP1\pgdata:/var/lib/postgresql/data" ^
    mon-image-postgres
  ```

- **Lancer Adminer pour accéder à la base :**
  ```
  docker run -d --name adminer --network app-network -p 8080:8080 adminer
  ```

1-4 Why do we need a multistage build? And explain each step of this dockerfile.

**Why use a multistage build?**

A multistage build allows you to separate the build environment (which needs tools like Maven and a JDK) from the runtime environment (which only needs a JRE).  
This results in a much smaller final image, containing only what is necessary to run the application, not to build it.  
It also improves security and reduces attack surface.

**Explanation of each step in the Dockerfile:**

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS myapp-build
ENV MYAPP_HOME=/opt/myapp
WORKDIR $MYAPP_HOME

RUN apk add --no-cache maven

COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests
```
- **FROM eclipse-temurin:21-jdk-alpine AS myapp-build**  
  Starts a build stage with a JDK (Java Development Kit) and gives it the name `myapp-build`.
- **ENV MYAPP_HOME=/opt/myapp**  
  Sets an environment variable for the app directory.
- **WORKDIR $MYAPP_HOME**  
  Sets the working directory inside the image.
- **RUN apk add --no-cache maven**  
  Installs Maven to build the project.
- **COPY pom.xml .**  
  Copies the Maven configuration file.
- **COPY src ./src**  
  Copies the source code.
- **RUN mvn package -DskipTests**  
  Builds the application and creates a JAR file, skipping tests.

```dockerfile
# Run stage
FROM eclipse-temurin:21-jre-alpine
ENV MYAPP_HOME=/opt/myapp
WORKDIR $MYAPP_HOME
COPY --from=myapp-build $MYAPP_HOME/target/*.jar $MYAPP_HOME/myapp.jar

ENTRYPOINT ["java", "-jar", "myapp.jar"]

- **FROM eclipse-temurin:21-jre-alpine**  
  Starts a new, clean image with only the Java Runtime Environment (JRE).
- **ENV MYAPP_HOME=/opt/myapp**  
  Sets the same environment variable for consistency.
- **WORKDIR $MYAPP_HOME**  
  Sets the working directory.
- **COPY --from=myapp-build $MYAPP_HOME/target/*.jar $MYAPP_HOME/myapp.jar**  
  Copies the built JAR from the build stage into the runtime image.
- **ENTRYPOINT ["java", "-jar", "myapp.jar"]**  
  Defines the command to run the application when the container starts.

**Summary:**  
The first stage builds the app, the second stage runs it with only the minimal dependencies.

1-5 Why do we need a reverse proxy?

A reverse proxy acts as a single point of entry for clients, redirects requests to the right backend services, improves security by hiding the internal architecture, enables SSL termination and load balancing, and can serve static files.
It therefore facilitates the management, security and scalability of the infrastructure.

1-6 Why is docker-compose so important?

Docker Compose is important because it allows you to define, configure, and run multi-container Docker applications easily. With a single YAML file, you can describe all your services, networks, and volumes, and manage them together with simple commands. This simplifies orchestration, ensures reproducibility, and makes it easy to set up complex development or production environments with minimal effort.

1-7 Document docker-compose most important commands.

- **docker-compose up -d --build**  
  Builds images (if needed) and starts all services in detached mode (in the background).

- **docker-compose down**  
  Stops and removes all containers, networks, and volumes created by `up`.

- **docker-compose ps**  
  Lists the running containers managed by the current Compose file.

- **docker-compose logs [service]**  
  Shows the logs for all services, or for a specific service if specified.

- **docker-compose stop**  
  Stops all running services without removing them.

- **docker-compose start**  
  Starts existing (stopped) services.

- **docker-compose restart [service]**  
  Restarts one or more services.

- **docker-compose build [service]**  
  Builds or rebuilds the images for the services.

- **docker-compose exec [service] bash**  
  Opens a shell inside a running service container (useful for debugging).

  1-8 Document your docker-compose file.

- **version: '3.8'**  
  Specifies the Compose file format version.

- **services:**  
  Defines the different containers (services) that make up your application.

  - **db:**  
    - **image:** The Docker image to use for the database (should be set to `postgres:15` or another valid image).
    - **environment:** Sets environment variables for the database container (user, password, database name).
    - **env_file:** Loads additional environment variables from the specified `.env` file.
    - **volumes:** Mounts a named volume (`db-data`) to persist database data.
    - **networks:** Connects the container to the `app-network`.
    - **restart:** Restarts the container unless it is explicitly stopped.

  - **simpleapi:**  
    - **build:** Builds the image from the specified directory containing the Dockerfile for your backend.
    - **image:** Names the built image as `simpleapi`.
    - **container_name:** Sets a custom name for the container.
    - **env_file:** Loads environment variables from the `.env` file.
    - **networks:** Connects the container to both `app-network` and `app-network2`.
    - **depends_on:** Ensures the `db` service starts before this one.
    - **restart:** Restarts the container on failure, up to 3 times.

  - **http-server:**  
    - **build:** Builds the image from the specified directory containing the Dockerfile for your Apache server.
    - **image:** Names the built image as `my-apache`.
    - **container_name:** Sets a custom name for the container.
    - **ports:** Maps port 80 of the container to port 80 on the host.
    - **networks:** Connects the container to `app-network2`.
    - **depends_on:** Ensures the `simpleapi` service starts before this one.
    - **restart:** No automatic restart (should be `restart: "no"` or omitted).

- **networks:**  
  - **app-network:** Custom network for inter-service communication.
  - **app-network2:** Another custom network for more granular control.

- **volumes:**  
  - **db-data:** Named volume to persist PostgreSQL data outside the container lifecycle.

1-9 Document your publication commands and published images in Docker Hub.

**Publication commands:**
- **Log in to Docker Hub (if not already logged in):**
  docker login

- **Push the image to Docker Hub:**
  docker push louisfrqt/my-apache:1.0

1-10 Why do we put our images into an online repo?

Putting Docker images into an online repository allows you to easily share, distribute, and deploy your images across different machines, teams, or environments. It ensures that everyone uses the same, consistent image version, simplifies collaboration, and enables automated deployments in CI/CD pipelines or cloud platforms. An online repo also serves as a backup and central source of truth for your container images.


TP2

2-1 What are testcontainers?

Testcontainers is a Java library 
 that allows you to run lightweight, throwaway Docker containers directly from your integration tests. It is mainly used to provide real, isolated dependencies during automated testing, ensuring that your tests run against actual services rather than mocks or in-memory alternatives. This improves test reliability and reproducibility, and makes it easier to test how your application interacts with external systems.

2-2 For what purpose do we need to use secured variables ?

 Secure variables are used to ensure that sensitive information is never exposed in code, logs or repositories, while enabling it to be used automatically in CI/CD workflows.

 2-3 Why did we put needs: test-backend on this job? Maybe try without this and you will see!

We use `needs: test-backend` to make sure that the `build-and-push-docker-image` job only runs **after** the backend tests have successfully completed. This ensures that we do not build and push Docker images if the code does not pass the tests, which helps maintain the quality and reliability of the images published to Docker Hub.

If you remove `needs: test-backend`, the image build and push job could start **before** or **at the same time as** the tests, meaning broken or untested code could be published. This breaks the CI/CD best practice of only deploying code that has passed all checks.

2-4 For what purpose do we need to push docker images?

Pushing Docker images to a remote registry allows you to share your application images with others, deploy them on different servers or cloud platforms, and use them in automated CI/CD pipelines. It ensures that the exact same image can be pulled and run anywhere, providing consistency, portability, and easy distribution of your application or service.

TP3 Ansible

3-1 Document your inventory and base commands
create the project

mkdir -p ~/mon-projet/ansible/inventories
cd ~/mon-projet/ansible/inventories
touch setup.yml

Write on the .yml
sudo nano setup.yml

Setup.yml :

all:
  vars:
    ansible_user: admin
    ansible_ssh_private_key_file: ~/.ssh/id_rsa
  children:
    prod:
      hosts:
        louis.fourquet.takima.cloud:

all: main group containing all hosts.
vars: global variables applicable to all hosts.
children: subgroups, in this case prod.
hosts: hosts belonging to the prod.

3-2 Document your playbook

- hosts: all
  gather_facts: true
  become: true

  roles:
    - docker

This playbook is designed to automate the installation and configuration of Docker on Debian machines.
The playbook uses an Ansible role called docker, which contains all the tasks involved in installing and configuring Docker.

Is it really safe to deploy automatically every new image on the hub ? explain. What can I do to make it more secure?

No, it is **not always safe** to automatically deploy every new image pushed to Docker Hub.

Why?
- A new image might contain bugs, vulnerabilities, or misconfigurations that were not detected before deployment.
- If someone gains access to your Docker Hub account, they could push a malicious image that would be automatically deployed to your servers.
- Automated deployment without proper checks can lead to downtime or security incidents if something goes wrong.

How to make it more secure?
- Deploy only after passing all tests:** Ensure images are only deployed if all CI tests pass.
- Use signed images:** Enable Docker Content Trust or image signing to verify the authenticity of images.
- Restrict who can push:** Limit Docker Hub push permissions to trusted users or CI/CD systems.
- Manual approval:** Add a manual approval step before deployment in your workflow.
- Scan images for vulnerabilities:** Use tools like Trivy or Docker Hub’s built-in scanning before deploying.
- Tag and deploy only stable releases:** Deploy only images with specific tags, not every `latest` or development tag.

Summary: 
Automatic deployment is powerful but risky if not controlled. Always combine it with strong testing, access control, and a manual or automated approval step.


