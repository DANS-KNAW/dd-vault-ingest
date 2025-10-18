dd-vault-ingest
===============

Service that processes DANS bag deposits converting them to RDA compliant BagPacks and sends them to the DANS Data Vault.

Purpose
-------
This service is part of the [Vault as a Service]{:target="_blank"} pipeline. It is responsible for:

* Validating incoming DANS deposits
* Converting DANS deposits to RDA compliant BagPacks
* Handing the BagPacks over to [Transfer Service]{:target="_blank"} for ingestion into the DANS Data Vault

Interfaces
----------
This service has the following interfaces:

![](img/overview.png){width="70%"}

### Provided interfaces

#### Inbox

* _Protocol type_: Shared filesystem
* _Internal or external_: **internal**
* _Purpose_: to receive DANS bag deposits from the SWORDv2 deposit service

#### Admin console

* _Protocol type_: HTTP
* _Internal or external_: **internal**
* _Purpose_: application monitoring and management

### Consumed interfaces

#### Vault Catalog

* _Protocol type_: HTTP
* _Internal or external_: **internal**
* _Purpose_: register skeleton dataset version records for incoming deposits 

#### Validate DANS Bag

* _Protocol type_: HTTP
* _Internal or external_: **internal**
* _Purpose_: validate incoming DANS bag deposits

Processing
----------

The service continuously monitors its inbox for new DANS bag deposits. When a new deposit arrives, the service performs the following steps:

1. Validate the DANS bag using the Validate DANS Bag service.
2. If the bag is valid, create a skeleton dataset version record in the Vault Catalog.
3. Convert the DANS bag to an RDA compliant BagPack.
4. Hand over the BagPack to the Transfer Service for ingestion into the DANS Data Vault.

[Vault as a Service]: {{ vault_as_a_service_url }}
[Transfer Service]: {{ transfer_service_url }}
