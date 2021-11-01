## How to create modules
- Create Empty Project, press Ok when ProjectStructure window is showed
- Click New..From Existing sources, select the previous created folder
- Click on the root foolder path and create a new settings.gradle
paste include ':module' for each moduel you create and sync on the left gradle panel, removing the non used
- To create other modules just click new module from the root path
- On the gradle panel: if all modules Builds are successfully precessed, 
the module will be added on the gradle tree, If the new module is not under the root node (web-app-2) just UNLINK the service outside.


- TODO: quando c'è un errore nei topic, la prossima chiamata che scatenerà un evento non funziona (non triggera e non viene gestita). CORREGGERLA
- SOLO qunado faccio la purge dei containers e immagini funziona
-Occhio al cambio scheda
- Occhio al timeout del singolo topic -> MOTIVO

-SEE MESSAGES FROM TOPIC
-docker-compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic wallet --from-beginning --timeout-ms 1000

- SEE MESSAGES FROM PARTITION 0 OF ORDER TOPIC
- docker-compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic order --from-beginning --timeout-ms 1000 --partition 0

- MONITOR AND LIST ALL PARTITION AND OTHER INFO OFR A SPECIFIC TOPIC
- docker-compose exec kafka /opt/kafka/bin/kafka-topics.sh --describe --zookeeper zookeeper --topic wallet

- LIST TOPICS 
- docker-compose exec kafka /opt/kafka/bin/kafka-topics.sh --zookeeper zookeeper --list
