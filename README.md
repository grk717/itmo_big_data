# ITMO Big Data Homework#1
## Конфигурация и запуск Hadoop кластера
Файл конфигурации: [link](docker-compose.yml)

NameNode в рабочем состоянии:

![Namenode](screenshots/namenode.jpg?raw=true "Namenode")

ResourceManager в рабочем состоянии:

![NameNode](screenshots/resourcemanager.jpg?raw=true "Namenode")

## Вычисление среднего и дисперсии в парадигме MapReduce

1. Установка Python руками на всех нодах кроме historyserver 

    TODO: конфигурация докера для автоматической установки.
2. Перенос csv файла Local -> NameNode -> HDFS

    ```
    docker cp AB_NYC_2019.csv namenode:/
    docker exec -it namenode /bin/bash
    hdfs dfs -put AB_NYC_2019.csv /
    ```
3. Написание mapper и reducer скриптов

    [Mean Mapper](mapper_mean.py)
    
    [Mean Reducer](reducer_mean.py)
    
    [Variance Mapper](mapper_var.py)
    
    [Variance Reducer](reducer_var.py)

4. Копирование исполняемых файлов на NameNode

    ```
    sudo docker cp /home/grk/PycharmProjects/test namenode:/
    cd test
    ```

5. Запуск процедуры MapReduce
    ```
    hadoop jar /opt/hadoop-3.2.1/share/hadoop/tools/lib/hadoop-streaming-3.2.1.jar -file mapper_mean.py -mapper mapper_mean.py -file reducer_mean.py -reducer reducer_mean.py -input /AB_NYC_2019.csv -output output24


    hadoop jar /opt/hadoop-3.2.1/share/hadoop/tools/lib/hadoop-streaming-3.2.1.jar -file mapper_var.py -mapper mapper_var.py -file reducer_var.py -reducer reducer_var.py -input /AB_NYC_2019.csv -output output23
    ```
    TODO: разобраться как разрешить Hadoop писать результаты в одну папку, а то видно с какого раза всё заработало.

6. Перенос результатов из hdfs на host machine

    ```
    root@e440227780e5:/test# hadoop fs -get /user/root/output24/part-00000 /mean_result
    root@e440227780e5:/test# hadoop fs -get /user/root/output23/part-00000 /var_result

    grk@itmo:~/docker-hadoop$ sudo docker cp namenode:/mean_result /root
    grk@itmo:~/docker-hadoop$ sudo docker cp namenode:/var_result /root
    ```
7. Проверка результатов вычислений
    ```
    grk@itmo:~/PycharmProjects/hadoop$ pip3 install -r requirements.txt
    grk@itmo:~/PycharmProjects/hadoop$ python3 main.py
    ```
## Результаты
[Вычисление дисперсии и среднего через pandas](results_pandas)

[Вычисление количества строк и среднего через MapReduce](mean_result)

[Вычисление дисперсии приближенным и точным методами через MapReduce](var_result) 