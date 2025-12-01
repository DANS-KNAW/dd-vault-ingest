Local debugging
===============

To locally debug you need to have the following services running:

* [dd-validate-dans-bag]{:target="_blank"}. Note that its `validation.baseFolder` configuration property should point to the inbox or an ancestor of it.
* [dd-vault-catalog]{:target="_blank"}. You __could__ run this in local debug mode as well, but the HQSLDB is very limited, so it is better to use the catalog
  installed on the development Vagrant box.


[dd-validate-dans-bag]: {{ dd_validate_dans_bag_url }}
[dd-vault-catalog]: {{ dd_vault_catalog_url }}