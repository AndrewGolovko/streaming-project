# Streaming course final project assignment

### team-maxandrmyk

- Andrii Holovko
- Maksym Tarnavskyi
- Mykola Trokhymovych


## Build

To build all of the components - simply run `sbt docker` from the root project folder.


## Deploy

### Local

To deploy local environment and start testing - simply run `docker-compose up` from the root project folder.

### Staging

#### Configure

 - Install [ecs-cli](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html)

 - Configure cli and login to ecr to be able to push images. You can simply use provided script:
   ```
   ./staging_configure.sh <AWS_ACCESS_KEY_ID> <AWS_SECRET_ACCESS_KEY>
   ```

#### Build and push docker images to ECR

   After building an image with `sbt docker` you can do `sbt dockerPush` to push. Or you can do both with `dockerBuildAndPush`

#### Deployment

We are going to operate with four services separately:
   - `weather-provider` - generator of weather data
   - `driver-rides-emulator` - generator of taxi rides data
   - `streaming-app` - streaming application
   - `fare-prediction-app` - predicts fare for rides

   You should specify as a first argument name of the service (same names as specified above) and then follows command like `up`, `ps`, `start`, `stop`, `down`, `scale 3`.

   - to start your service:
   ```
   ./staging_compose.sh <name-of-the-service> up
   ```
   - do not forget to stop and clean up:
   ```
   ./staging_compose.sh <name-of-the-service> rm --delete-namespace
   ```
   - of course you can just stop without deleting the service and then start again using `start` and `stop` commands
   - to scale:
   ```
   ./staging_compose.sh <name-of-the-service> scale 2
   ```
   - list running containers
   ```
   ./staging_compose.sh <name-of-the-service> ps
   ```

#### Logs and debugging your app

   This is essential for you to debug. Having the output of the `staging_compose ps` command with taskId, you can access logs of your service run like this:
   ```
   ecs-cli logs --task-id 6ef4bc73-9bed-499c-91ee-390da6d2a851 --follow
   ```

#### Interacting with Kafka

   At the moment access to kafka is managed from single client ec2 machine. To log in to the machine ask teacher to provide you a pem file and put it to the project root.

   To log in to that client machine simply run
   ```
   ./kafka_client.sh
   ```
   Once logged in - you can run commands below from the home dir.

   Basically, you need 3 types of operations:

   - create/describe/list topics
     - create
     ```
     kafka-topics.sh --zookeeper z-3.ucustreamingclass.mf5zba.c7.kafka.us-east-1.amazonaws.com:2181,z-2.ucustreamingclass.mf5zba.c7.kafka.us-east-1.amazonaws.com:2181,z-1.ucustreamingclass.mf5zba.c7.kafka.us-east-1.amazonaws.com:2181 --create --topic test_topic_out --replication-factor 3 --partitions 2
     ```
     - describe
     ```
     kafka-topics.sh --zookeeper z-3.ucustreamingclass.mf5zba.c7.kafka.us-east-1.amazonaws.com:2181,z-2.ucustreamingclass.mf5zba.c7.kafka.us-east-1.amazonaws.com:2181,z-1.ucustreamingclass.mf5zba.c7.kafka.us-east-1.amazonaws.com:2181 --describe --topic test_topic_out
     ```
     - list
     ```
     kafka-topics.sh --zookeeper z-3.ucustreamingclass.mf5zba.c7.kafka.us-east-1.amazonaws.com:2181,z-2.ucustreamingclass.mf5zba.c7.kafka.us-east-1.amazonaws.com:2181,z-1.ucustreamingclass.mf5zba.c7.kafka.us-east-1.amazonaws.com:2181 --list
     ```
   - consume topic
     ```
     kafka-console-consumer.sh --bootstrap-server z-3.ucustreamingclass.mf5zba.c7.kafka.us-east-1.amazonaws.com:2181,z-2.ucustreamingclass.mf5zba.c7.kafka.us-east-1.amazonaws.com:2181,z-1.ucustreamingclass.mf5zba.c7.kafka.us-east-1.amazonaws.com:2181 --topic sensor-data --from-beginning
     ```
   - produce into topic
     ```
     kafka-console-producer.sh --broker-list z-3.ucustreamingclass.mf5zba.c7.kafka.us-east-1.amazonaws.com:2181,z-2.ucustreamingclass.mf5zba.c7.kafka.us-east-1.amazonaws.com:2181,z-1.ucustreamingclass.mf5zba.c7.kafka.us-east-1.amazonaws.com:2181 --topic sensor-data
     ```

#### Hints

   When debugging `ecs-cli compose` task create outputs with --debug you may find useful piping through

   ```
   ... | awk '{gsub(/\\n/,"\n")}1'
   ```
   to substitute \n with actual newline


## Kafka

### Create topic

You can use confluent bundled tools to interact with the cluster, e.g. to create topics:

Command to create topic named foo with 4 partitions and replication-factor 1
```
docker run --net=host --rm confluentinc/cp-kafka:5.1.0 kafka-topics --create --topic foo --partitions 4 --replication-factor 1 --if-not-exists --zookeeper localhost:2181
```

# Components

- `weather-provider` - generator of weather data
- `driver-rides-emulator` - generator of taxi rides data
- `streaming-app` - streaming application
- `fare-prediction-app` - predicts fare for rides


# Scaling streaming app

Scaling kafka-streams is as easy as pie - just start one more instance of it. As we use docker-compose we can do so by executing:
```
docker-compose scale streaming-app=4
```

Keep in mind that maximum parallelism level is number of partitions in the topic that streaming app consumes.
