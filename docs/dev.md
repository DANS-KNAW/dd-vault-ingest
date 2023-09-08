Development
===========
This page contains information for developers about how to contribute to this project.

Set-up
------
This project can be used in combination with  [dans-dev-tools]{:target=_blank}. 
Before you can start it as a service some dependencies must first be started.
For one dependency you have the choice between a Virtual Machine (VM) `dev_transfer`
and a local service `dd-vault-catalog`. For the VM you need access to the project [dd-dtap]{:target=_blank},
the local service requires a local database.

[dans-dev-tools]: https://github.com/DANS-KNAW/dans-dev-tools#readme
[dd-dtap]: https://github.com/DANS-KNAW/dd-dtap#readme
[dd-validate-dans-bag]: https://github.com/DANS-KNAW/dd-validate-dans-bag#readme
[dd-vault-catalog]: https://github.com/DANS-KNAW/dd-vault-catalog#readme

### Initialize development environment

This is only necessary once per project. If you execute this any existing configuration and data will be reset.

Open separate terminal tabs for `dd-vault-ingest-flow`, its dependency [dd-validate-dans-bag]
and the optional dependency [dd-vault-catalog]. In each tab run:

```commandline
start-env.sh
```

The service `dd-validate-dans-bag` needs different configurations for `dd-vault-ingest-flow` and other services. 
So you will have to update the generated `dd-validate-dans-bag/etc/config.yml`,
perhaps keep copies of the files or comment lines to switch between both situations.

* remove one level from `../../` in the schema locations.
* `dataverse: null`
* `vaultCatalog.baseUrl: https://dev.transfer.dans-data.nl/vault-catalog`

You will also have to adjust `dd-vault-ingest-flow/etc/config.yml`:

* vaultCatalog.url: https://dev.transfer.dans-data.nl

Both URLs in the above configuration examples assume you use the VM `dev_transfer`, 
if not, keep the URLs as generated from `src/test/resources/debug-etc`.

### Start services

To start the VM run in the root of `dd-dtap`:

```commandline
start-preprovisioned-box.py -s dev_transfer
```

Without the VM you will need a local database for the service `dd-vault-catalog`, run in a separate terminal tab:

```commandline
start-hsqldb-server.sh
```

Open terminal tabs for the services `dd-vault-ingest-flow`, `dd-validate-dans-bag`, optionally `dd-vault-catalog` and run:

```commandline
start-service.sh
```

### Create a deposit

* Create a directory with a `<UUID>` as name.
* Copy a valid bag into that directory, for example:

        dd-dans-sword2-examples/src/main/resources/example-bags/valid/default-open

  Note that `all_mappings` has an invalid prefix for `Has-Organizational-Identifier` in `bag-info.txt`,
  other bags should be valid.

  * Create a file in the same directory named `deposit.properties`, an example for the content:

        bag-store.bag-id = <UUID>
        dataverse.bag-id = urn:uuid:<UUID>
        dataverse.sword-token = sword:<UUID>
        creation.timestamp = 2023-08-16T17:40:41.390209+02:00
        deposit.origin = SWORD2
        depositor.userId = user001
        bag-store.bag-name = default-open

    Note that 
    * The `bag-name` should match the copied bag.
    * The `userId` should match a value configured as a `dataSupplier` in `dd-vault-ingest-flow/etc/config.yml`.
    * The `<UUID>` should match the directory name

## Start an ingest

To start an ingest, move (not copy, otherwise the processing might start before the copy the completed)
a deposit into one of the inboxes configured in:

    dd-vault-ingest-flow/etc/config.yml

You can examine details of the result on the VM in `/var/opt/dans.knaw.nl/tmp/ocfl-tar/inbox`
and the database: 

    sudo su postgres
    psql dd_vault_catalog
    select bag_id, data_supplier from ocfl_object_versions;
    \c dd_transfer_to_vault
    select bag_id, data_supplier from transfer_item;
