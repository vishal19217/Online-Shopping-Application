CREATE TABLE IF NOT EXISTS `t_inventory` (
    `id`       BIGINT      NOT NULL AUTO_INCREMENT,
    `skucode`  VARCHAR(25) DEFAULT NULL,
    `quantity` INT         DEFAULT NULL,
    PRIMARY KEY (`id`)
);
