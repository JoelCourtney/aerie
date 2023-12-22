# Environment

This document provides detailed information about environment variables for each service in Aerie.

- [Aerie Gateway](#aerie-gateway)
- [Aerie Merlin](#aerie-merlin)
- [Aerie Scheduler](#aerie-scheduler)
- [Aerie Sequencing](#aerie-sequencing)
- [Aerie UI](#aerie-ui)
- [Hasura](#hasura)
- [Postgres](#postgres)

## Aerie Gateway

See the [environment variables document](https://github.com/NASA-AMMOS/aerie-gateway/blob/develop/docs/ENVIRONMENT.md) in the Aerie Gateway repository.

## Aerie Merlin

| Name                                  | Description                                                                                                                 | Type     | Default                         |
| ------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- | -------- | ------------------------------- |
| `JAVA_OPTS`                           | Configuration for Merlin's logging level and output file                                                                    | `string` | log level: warn. output: stderr |
| `MERLIN_PORT`                         | Port number for the Merlin server                                                                                           | `number` | 27183                           |
| `MERLIN_LOCAL_STORE`                  | Local storage for Merlin in the container                                                                                   | `string` | /usr/src/app/merlin_file_store  |
| `MERLIN_DB_SERVER`                    | The DB instance that Merlin will connect with                                                                               | `string` |                                 |
| `MERLIN_DB_PORT`                      | The DB instance port number that Merlin will connect with                                                                   | `number` | 5432                            |
| `MERLIN_DB_USER`                      | Username of the DB instance                                                                                                 | `string` |                                 |
| `MERLIN_DB_PASSWORD`                  | Password of the DB instance                                                                                                 | `string` |                                 |
| `MERLIN_DB`                           | The DB for Merlin.                                                                                                          | `string` | aerie_merlin                    |
| `UNTRUE_PLAN_START`                   | Temporary solution to provide plan start time to models, should be set to a time that models will not fail to initialize on | `string` |                                 |
| `ENABLE_CONTINUOUS_VALIDATION_THREAD` | Flag to enable a worker thread that continously computes and caches activity directive validation results                   | `boolean`| true                            |
| `VALIDATION_THREAD_POLLING_PERIOD`    | Number of milliseconds the above worker thread should wait before querying the database for new, unvalidated directives     | `string` | 500                             |

## Aerie Merlin Worker

| Name                        | Description                                                                                                                 | Type     | Default                                      |
| --------------------------- | --------------------------------------------------------------------------------------------------------------------------- | -------- | -------------------------------------------- |
| `JAVA_OPTS`                 | Configuration for Merlin's logging level and output file                                                                    | `string` | log level: warn. output: stderr              |
| `MERLIN_WORKER_LOCAL_STORE` | The local storage as for the Merlin container                                                                               | `string` | /usr/src/app/merlin_file_store               |
| `MERLIN_WORKER_DB_SERVER`   | The DB instance that Merlin will connect with                                                                               | `string` | (this must the same as the Merlin container) |
| `MERLIN_WORKER_DB_PORT`     | The DB instance port number that Merlin will connect with                                                                   | `number` | (this must the same as the Merlin container) |
| `MERLIN_WORKER_DB_USER`     | Username of the DB instance                                                                                                 | `string` | (this must the same as the Merlin container) |
| `MERLIN_WORKER_DB_PASSWORD` | Password of the DB instance                                                                                                 | `string` | (this must the same as the Merlin container) |
| `MERLIN_WORKER_DB`          | The DB for Merlin.                                                                                                          | `string` | (this must the same as the Merlin container) |
| `SIMULATION_PROGRESS_POLL_PERIOD_MILLIS`          | Cadence at which the worker will report simulation progress to the database.                                                | `number` | 5000                                         |
| `UNTRUE_PLAN_START`         | Temporary solution to provide plan start time to models, should be set to a time that models will not fail to initialize on | `string` |                                              |

## Aerie Scheduler

| Name                          | Description                                                     | Type     | Default                         |
| ----------------------------- | --------------------------------------------------------------- | -------- | ------------------------------- |
| `HASURA_GRAPHQL_ADMIN_SECRET` | The admin secret for Hasura which gives admin access if used.   | `string` |                                 |
| `JAVA_OPTS`                   | Configuration for the scheduler's logging level and output file | `string` | log level: warn. output: stderr |
| `MERLIN_GRAPHQL_URL`          | URI of the Merlin graphql interface to call                     | `string` | http://hasura:8080/v1/graphql   |
| `SCHEDULER_DB`                | The DB for scheduler                                            | `string` | aerie_scheduler                 |
| `SCHEDULER_DB_PASSWORD`       | Password of the DB instance                                     | `string` |                                 |
| `SCHEDULER_DB_PORT`           | The DB instance port number that scheduler will connect with    | `number` | 5432                            |
| `SCHEDULER_DB_SERVER`         | The DB instance that scheduler will connect with                | `string` |                                 |
| `SCHEDULER_DB_USER`           | Username of the DB instance                                     | `string` |                                 |
| `SCHEDULER_PORT`              | Port number for the scheduler server                            | `number` | 27185                           |

## Aerie Scheduler Worker

| Name                          | Description                                                           | Type     | Default                                            |
| ----------------------------- | --------------------------------------------------------------------- | -------- | -------------------------------------------------- |
| `HASURA_GRAPHQL_ADMIN_SECRET` | The admin secret for Hasura which gives admin access if used.         | `string` |                                                    |
| `JAVA_OPTS`                   | Configuration for the scheduler's logging level and output file       | `string` | log level: warn. output: stderr                    |
| `MERLIN_GRAPHQL_URL`          | URI of the Merlin graphql interface to call                           | `string` | http://hasura:8080/v1/graphql                      |
| `MERLIN_LOCAL_STORE`          | Local storage for Merlin in the container (for backdoor jar access)   | `string` | /usr/src/app/merlin_file_store                     |
| `SCHEDULER_DB`                | The DB for scheduler                                                  | `string` | aerie_scheduler                                    |
| `SCHEDULER_DB_PASSWORD`       | Password of the DB instance                                           | `string` |                                                    |
| `SCHEDULER_DB_PORT`           | The DB instance port number that scheduler will connect with          | `number` | 5432                                               |
| `SCHEDULER_DB_SERVER`         | The DB instance that scheduler will connect with                      | `string` |                                                    |
| `SCHEDULER_DB_USER`           | Username of the DB instance                                           | `string` |                                                    |
| `SCHEDULER_OUTPUT_MODE`       | how scheduler output is sent back to aerie                            | `string` | UpdateInputPlanWithNewActivities                   |
| `SCHEDULER_RULES_JAR`         | Jar file to load scheduling rules from (until user input to database) | `string` | /usr/src/app/merlin_file_store/scheduler_rules.jar |

## Aerie Sequencing

| Name                          | Description                                                   | Type     | Default                            |
| ----------------------------- | ------------------------------------------------------------- | -------- | ---------------------------------- |
| `HASURA_GRAPHQL_ADMIN_SECRET` | The admin secret for Hasura which gives admin access if used. | `string` |                                    |
| `LOG_FILE`                    | Either an output filepath to log to, or 'console'             | `string` | console                            |
| `LOG_LEVEL`                   | Logging level for filtering logs                              | `string` | warn                               |
| `MERLIN_GRAPHQL_URL`          | URI of the Aerie GraphQL API                                  | `string` | http://hasura:8080/v1/graphql      |
| `SEQUENCING_DB`               | Name of sequencing Postgres database                          | `string` | aerie_sequencing                   |
| `SEQUENCING_DB_SERVER`        | Hostname of Postgres instance                                 | `string` |                                    |
| `SEQUENCING_DB_PASSWORD`      | Password of Postgres instance                                 | `string` |                                    |
| `SEQUENCING_DB_PORT`          | Port of Postgres instance                                     | `number` | 5432                               |
| `SEQUENCING_DB_USER`          | User of Postgres instance                                     | `string` |                                    |
| `SEQUENCING_LOCAL_STORE`      | Local storage file storage in the container                   | `string` | /usr/src/app/sequencing_file_store |
| `SEQUENCING_SERVER_PORT`      | Port the server listens on                                    | `number` | 27184                              |

## Aerie UI

See the [environment variables document](https://github.com/NASA-AMMOS/aerie-ui/blob/develop/docs/ENVIRONMENT.md) in the Aerie UI repository.

## Hasura

| Name                            | Description                                                   | Type     |
| ------------------------------- | ------------------------------------------------------------- | -------- |
| `AERIE_MERLIN_DATABASE_URL`     | Url of the Merlin Postgres database.                          | `string` |
| `AERIE_MERLIN_URL`              | Url of the Merlin service.                                    | `string` |
| `AERIE_SCHEDULER_DATABASE_URL`  | Url of the scheduler Postgres database.                       | `string` |
| `AERIE_SCHEDULER_URL`           | Url of the scheduler service.                                 | `string` |
| `AERIE_SEQUENCING_DATABASE_URL` | Url of the sequencing Postgres database.                      | `string` |
| `AERIE_SEQUENCING_URL`          | Url of the sequencing service.                                | `string` |
| `AERIE_UI_DATABASE_URL`         | Url of the UI Postgres database                               | `string` |
| `HASURA_GRAPHQL_ADMIN_SECRET`   | The admin secret for Hasura which gives admin access if used. | `string` |
| `HASURA_GRAPHQL_JWT_SECRET`     | The JWT secret for JSON web token auth. Also in Gateway.      | `string` |

Additionally, Hasura provides documentation on it's own environment variables you can use to fine-tune your deployment:

1. [graphql-engine](https://hasura.io/docs/latest/graphql/core/deployment/graphql-engine-flags/reference.html#server-flag-reference)
1. [metadata and migrations](https://hasura.io/docs/latest/graphql/core/migrations/advanced/auto-apply-migrations.html#applying-migrations)

## Postgres

The default Aerie deployment uses the default Postgres environment. See the [Docker Postgres documentation](https://hub.docker.com/_/postgres) for more complete information on those environment variables and how to use them.

[svelte-kit-adapter-node-docs]: https://github.com/sveltejs/kit/blob/master/packages/adapter-node/README.md
