package com.sack.rpgroll.ascension.deferred;

/** Qué tipo de contenido esconde un desbloqueo secreto. */
public enum SecretTargetType {

    /** Una raza de RPGRoll: no se puede elegir hasta cumplir los requisitos. */
    RACE,
    /** Una clase de RPGRoll: igual que la raza. */
    CLASS,
    /** Un rasgo de RPGRoll: se concede solo al cumplir los requisitos. */
    TRAIT,
    /** Una especialización de Ascension: no se puede tomar hasta cumplirlos. */
    SPECIALIZATION

}
