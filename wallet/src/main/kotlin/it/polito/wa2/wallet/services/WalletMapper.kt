package it.polito.wa2.wallet.services

import it.polito.wa2.api.core.wallet.Wallet
import it.polito.wa2.wallet.domain.WalletEntity
//import org.mapstruct.Mapper
//import org.mapstruct.Mapping
//import org.mapstruct.Mappings


//@Mapper(componentModel = "spring")
interface WalletMapper {
    // TODO: ask francesco - errore se tolgo commenti dopo modifca a WalletEntity
   // @Mappings(Mapping(target = "serviceAddress", ignore = true))
   // fun entityToApi(entity: WalletEntity): Wallet

   // @Mappings(Mapping(target = "id", ignore = true))
   // fun apiToEntity(api: Wallet): WalletEntity
}

