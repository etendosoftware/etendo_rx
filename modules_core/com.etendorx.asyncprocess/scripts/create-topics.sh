<<<<<<< HEAD
echo "Waiting for Kafka to come online..."

cub kafka-ready -b kafka:9092 1 20

=======
>>>>>>> origin/develop
# create the users topic
kafka-topics \
  --bootstrap-server kafka:9092 \
  --topic async-process-execution \
  --replication-factor 1 \
  --partitions 4 \
  --create

kafka-topics \
  --bootstrap-server kafka:9092 \
  --topic async-process \
  --replication-factor 1 \
  --partitions 4 \
  --create


kafka-topics \
  --bootstrap-server kafka:9092 \
  --topic rejected-process \
  --replication-factor 1 \
  --partitions 4 \
  --create

sleep infinity
