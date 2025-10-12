# Поисковый движок | Search Engine

## 📌 О проекте

Это REST API-приложение, представляющее собой поисковый движок.  
Проект индексирует веб-сайты, указанные в конфигурации, и производит поиск по проиндексированным страницам.
---
# 🎨 Front-End

- **Dashboard**:

  Отображает статистику по выполняемой индексации, а так же статус индексируемых сайтов.

- **Management**:

  Инструменты управления поисковым движком:
    - запуск/остановка полной индексации
    - добавление/обновление отдельной страницы в индекс

- **Search**:

  Страница для тестирования поискового движка, содержит выпадающий список с выбором сайта для поиска и кнопку «Найти», показывающую результаты.

---

# ⚙️ Back-End

### 📋 Используемый стек технологий

- Java 17 (Collection, CompletableFuture, ForkJoinPool etc)
- Maven v3.9.6
- Spring Boot v3.3.2
- Spring Data JPA
- Spring Web (REST API)
- Spring Thymeleaf
- MySQL v8.0.33
- MapStruct v1.5.5.Final
- Jsoup v1.18.1
- Liquibase v4.29.0
- Lombok v1.18.34

---

## 📂 Настройка и локальный запуск проекта

<details>
<summary>Через командную строку</summary>

1. Клонировать репозиторий:
    ```java
    git clone https://github.com/Pussaley/search-engine.git
    ```

2. Перейти в директорию проекта:

    ```java
    cd <путь_до_папки_с_проектом>
    ```

3. Собрать проект:

    ```java
    mvn clean package
    ```

4. Укажите в файле-конфигурации [application.yaml](src/main/resources/application.yaml) актуальные настройки
   соединения к Базе данных и желаемый профиль (по умолчанию включен профиль prod). Затем укажите в
   в файле-конфигурации [application-prod.yaml](src/main/resources/application-prod.yaml) желаемый список сайтов.


5. Запустите приложение:
    ```java
    java -jar -Dspring.profiles.active=prod target/SearchEngine-*.jar
    ```

</details>


<details>
<summary>Через IntelliJ IDEA</summary>

1. Запустите IntelliJ IDEA
2. Клонируйте репозиторий:
   
   1. Выберите File → New → Project from Version Control
   2. Введите URL репозитория:
       ```java
        https://github.com/Pussaley/search-engine.git
        ``` 
   3. Укажите директорию для сохранения проекта
   4. Нажмите "Clone"
3. Дождитесь завершения клонирования и автоматической индексации проекта
4. После открытия проекта дождитесь полной загрузки и индексации Maven-зависимостей
5. Укажите в [application.yaml](src/main/resources/application.yaml) актуальные настройки
   соединения к Базе данных и желаемый профиль (по умолчанию включен профиль prod)
6. Запустите главный класс приложения:
    1. Перейдите в класс [Application.java](src/main/java/searchengine/Application.java) (может потребоваться устанвить JDK для запуска проекта)
    2. Нажмите ПКМ на класс → Run или используйте сочетание клавиш Ctrl+Shift+F10

</details>