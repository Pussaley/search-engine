<style>
    details > summary {
        padding: 5px;
        background-color: #eee;
        color: #333;
        border: 1px #ccc solid;
        cursor: pointer;
        list-style: none;
    }
    details > div {
        border: 1px #ccc solid;
        padding: 10px;
    }
    details[open] > summary {
        color:#eee; 
        background-color:#333;
    }
    summary:before {
       content: "+";
       font-size: 20px;
       font-weight: bold;
       margin: -5px 5px 0 0;
    }
    details[open] summary:before {
       content: "-";
    }
</style>

# Поисковый движок | Search Engine

___

## Описание проекта:

**REST API** приложение<br>
Проект представляет собой поисковый движок,
который индексирует веб-сайты,
указанные в конфигурации,
и производит поиск по проиндексированным страницам.
____
<span style="font-size: 30px">Front-End:</span>

- <span style="font-size: 25px">Dashboard</span><br>
  Статистика и статус индексируемых сайтов.
  - <span style="font-size: 25px">Management</span><br>
    Инструменты управления поисковым движком:
      - <button style="background: #177db8">start/stop indexing</button> - запуск/остановка полной индексации
      - <button style="background: #177db8">add/update</button> - добавление отдельной страницы в индекс
  - <span style="font-size: 25px">Search</span><br>
      <div style="text-align: justify">Эта страница предназначена для тестирования поискового движка. 
      Присутствует выпадающий список с выбором сайта для поиска.
      При нажатии на кнопку «Найти» выводятся результаты поиска.</div>

------
<span style="font-size: 30px">Back-End:</span>

### *Используемый стек технологий*:

- Java 17 (в том числе Асинхронное программирование: CompletableFuture и ForkJoinPool)
- Maven v3.9.6
- Spring Boot v3.3.2
- MySQL v8.0.33
- MapStruct v1.5.5.Final
- Jsoup v1.18.1
- Liquibase v4.29.0

## Инструкция по локальному запуску Java Spring проекта из GitHub в IntelliJ

---

## 📂 Настройка и запуск проекта

<details>
<summary>Через командную строку</summary>

1. Склонировать [репозиторий](https://github.com/Pussaley/search-engine):

    ```java
    git clone https://github.com/Pussaley/search-engine.git
    ```

2. Перейдите в директорию проекта:

    ```java
    cd <путь до папки с проектом>
    ```

3. Соберите проект:

    ```java
    mvn clean package
    ```

4. Запустите приложение:

    ```java
    java -jar -Dspring.profiles.active=prod target/SearchEngine-*.jar
    ```

</details>
<br>
<details> <summary>Через IntelliJ IDEA</summary>

1. Клонирование репозитория через IntelliJ IDEA:

   1. Запустите IntelliJ IDEA
   2. Выберите File → New → Project from Version Control
   3. Введите URL репозитория:
       ```java
        https://github.com/Pussaley/search-engine.git
        ``` 
   4. Укажите директорию для сохранения проекта
   5. Нажмите "Clone"
2. Дождитесь завершения клонирования и автоматической индексации проекта
3. После открытия проекта дождитесь полной загрузки и индексации Maven-зависимостей
4. Укажите в [application.yaml](src/main/resources/application.yaml) актуальные настройки
 соединения к Базе данных и желаемый профиль (по умолчанию включен профиль prod)
5. Запустите главный класс приложения:
   1. Перейдите в класс [Application.java](src/main/java/searchengine/Application.java) (может потребоваться устанвить JDK для запуска проекта)
   2. Нажмите ПКМ на класс → Run или используйте сочетание клавиш Ctrl+Shift+F10

</details>
<p class="author" style="font-size: 8px; text-align: right;">Created by pussaley</p>