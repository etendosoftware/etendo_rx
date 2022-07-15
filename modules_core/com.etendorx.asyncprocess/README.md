### Consume changes
```shell
./bin/kafka-console-consumer \
  --topic bank-balances \
  --bootstrap-server localhost:9092 \
  --from-beginning \
  --property print.key=true \
  --property key.separator=" : " \
  --key-deserializer "org.apache.kafka.common.serialization.LongDeserializer" 
  --value-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
  
```
