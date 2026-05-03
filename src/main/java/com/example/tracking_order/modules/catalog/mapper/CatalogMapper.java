package com.example.tracking_order.modules.catalog.mapper;

import com.example.tracking_order.modules.catalog.dto.ProductDetailDto;
import com.example.tracking_order.modules.catalog.dto.ProductListDto;
import com.example.tracking_order.modules.catalog.entity.Category;
import com.example.tracking_order.modules.catalog.entity.Product;
import com.example.tracking_order.modules.catalog.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CatalogMapper {

    @Mapping(target = "id", source = "product.id")
    @Mapping(target = "basePrice", source = "product.basePrice")
    @Mapping(target = "salePrice", source = "product.salePrice")
    @Mapping(target = "inventory", source = "inventory")
    @Mapping(target = "category", source = "product.category")
    @Mapping(target = "seller", source = "product.seller")
    ProductListDto toProductListDto(Product product, Inventory inventory);

    @Mapping(target = "id", source = "product.id")
    @Mapping(target = "basePrice", source = "product.basePrice")
    @Mapping(target = "salePrice", source = "product.salePrice")
    @Mapping(target = "inventory", source = "inventory")
    @Mapping(target = "category", source = "product.category")
    @Mapping(target = "seller", source = "product.seller")
    ProductDetailDto toProductDetailDto(Product product, Inventory inventory);

    @Mapping(target = "quantityInStock", source = "quantityInStock")
    @Mapping(target = "quantityAvailable", expression = "java(inventory.getQuantityInStock() - inventory.getQuantityReserved())")
    ProductListDto.InventoryRef toInventoryRefList(Inventory inventory);

    @Mapping(target = "quantityInStock", source = "quantityInStock")
    @Mapping(target = "quantityAvailable", expression = "java(inventory.getQuantityInStock() - inventory.getQuantityReserved())")
    ProductDetailDto.InventoryRef toInventoryRefDetail(Inventory inventory);

    ProductListDto.CategoryRef toCategoryRefList(Category category);
    ProductDetailDto.CategoryRef toCategoryRefDetail(Category category);

    @Mapping(target = "name", source = "fullName")
    ProductListDto.SellerRef toSellerRefList(com.example.tracking_order.modules.user.entity.User user);
    
    @Mapping(target = "name", source = "fullName")
    ProductDetailDto.SellerRef toSellerRefDetail(com.example.tracking_order.modules.user.entity.User user);
}
