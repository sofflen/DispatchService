1. Start Kafka server
```shell
bin/kafka-server-start.sh config/kraft/server.properties
```
2. Start application in a new terminal window
```shell
mvn spring-boot:run
```
3. Start Producer, that sends to 'order.created' topic, in a new terminal window
```shell
bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic order.created
```
4. Start Consumer, that listens to 'order.dispatched' topic, in a new terminal window
```shell
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic order.dispatched
```
5. Send payload with console Producer
```json
{
  "orderId": "UUID",
  "item": "Object"
}
```