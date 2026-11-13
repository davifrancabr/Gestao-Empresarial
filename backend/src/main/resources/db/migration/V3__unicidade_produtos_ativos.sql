-- Nome e SKU só precisam ser únicos entre produtos ativos: um produto
-- arquivado não deve impedir o cadastro de um item com o mesmo nome.

drop index if exists ux_product_store_normalized_name;
drop index if exists ux_product_store_sku;

create unique index ux_product_store_normalized_name
    on product (store_id, normalized_name)
    where active;

create unique index ux_product_store_sku
    on product (store_id, lower(sku))
    where sku is not null and active;
