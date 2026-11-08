package com.example.bank

import java.time.LocalDate

enum class StatutDeCompte {
    CREATED,
    SUSPENDED,
    DELETED,
    BLOCKED
}

class SoldeInsuffisantException(message: String) : Exception(message)

class MontantInvalideException(message: String) : Exception(message)

class StatutInvalideException(message: String) : Exception(message)

open class Operation(val numero: Int, val dateOperation: LocalDate, val montant: Double)

class Versement(numero: Int, dateOperation: LocalDate, montant: Double) : Operation(numero, dateOperation, montant)

class Retrait(numero: Int, dateOperation: LocalDate, montant: Double) : Operation(numero, dateOperation, montant)

interface ICompte {
    fun verser(montant: Double)
    fun retirer(montant: Double)
    fun versement(montant: Double, compte: Compte)
    fun consulterSolde(): Double
    fun updateSolde()
    fun listeOperation(): MutableList<Operation>
    fun totalVersement(): Double
    fun totalRetrait(): Double
}

abstract class Compte(val code: Int, soldeDepart: Double) : ICompte {

    var solde: Double = soldeDepart
        protected set

    var statut: StatutDeCompte = StatutDeCompte.CREATED

    protected val operations: MutableList<Operation> = mutableListOf()

    override fun verser(montant: Double) {
        if (montant <= 0.0) {
            throw MontantInvalideException("Le montant doit etre positif")
        }

        solde = solde + montant

        val numero = operations.size + 1

        operations.add(Versement(numero, LocalDate.now(), montant))
    }

    abstract override fun retirer(montant: Double)

    override fun versement(montant: Double, compte: Compte) {
        retirer(montant)
        compte.verser(montant)
    }

    override fun consulterSolde(): Double {
        return solde
    }

    abstract override fun updateSolde()

    override fun listeOperation(): MutableList<Operation> {
        return operations
    }

    override fun totalVersement(): Double {
        var total = 0.0

        for (op in operations) {
            if (op is Versement) {
                total = total + op.montant
            }
        }

        return total
    }

    override fun totalRetrait(): Double {
        var total = 0.0

        for (op in operations) {
            if (op is Retrait) {
                total = total + op.montant
            }
        }

        return total
    }

    fun changerStatut(nouveauStatut: StatutDeCompte) {

        val ancienStatut = statut

        if (ancienStatut == StatutDeCompte.DELETED) {
            throw StatutInvalideException("Un compte supprime ne peut plus changer de statut")
        }

        if (ancienStatut == nouveauStatut) {
            throw StatutInvalideException("Le compte est deja $ancienStatut")
        }

        statut = nouveauStatut

        println("Le statut du compte numero $code change")
        println("Ancien statut : $ancienStatut")
        println("Nouveau statut : $nouveauStatut")
    }
}

class CompteCourant(code: Int, soldeDepart: Double, val decouvert: Double) : Compte(code, soldeDepart) {

    override fun retirer(montant: Double) {

        if (montant <= 0.0) {
            throw MontantInvalideException("Le montant doit etre positif")
        }

        if (solde - montant < -decouvert) {
            throw SoldeInsuffisantException("Retrait impossible : decouvert depasse")
        }

        solde = solde - montant

        val numero = operations.size + 1

        operations.add(Retrait(numero, LocalDate.now(), montant))
    }

    override fun updateSolde() {
        println("Un compte courant n'a pas d'interets, donc le solde ne change pas")
    }
}

class CompteEpargne(code: Int, soldeDepart: Double, val taux: Double) : Compte(code, soldeDepart) {

    override fun retirer(montant: Double) {

        if (montant <= 0.0) {
            throw MontantInvalideException("Le montant doit etre positif")
        }

        if (solde < montant) {
            throw SoldeInsuffisantException("retrait impossible solde insuffisant")
        }

        solde = solde - montant

        val numero = operations.size + 1

        operations.add(Retrait(numero, LocalDate.now(), montant))
    }

    override fun updateSolde() {
        val interets = solde * taux / 100
        solde = solde + interets
    }
}

fun Compte.resume(): String {
    return "Compte [code: $code] - ${listeOperation().size} operations effectuees, Solde actuel: $solde"
}

fun creerCompteCourantAvecVersement(
    code: Int,
    soldeDepart: Double,
    decouvert: Double,
    premierVersement: Double
): CompteCourant {

    val compte = CompteCourant(code, soldeDepart, decouvert).apply {
        verser(premierVersement)
    }

    return compte
}

fun afficherComptesParType(comptes: MutableList<Compte>, type: String) {

    for (compte in comptes) {

        if (type == "courant" && compte is CompteCourant) {
            println(compte.resume())
        }

        if (type == "epargne" && compte is CompteEpargne) {
            println(compte.resume())
        }
    }
}

fun main() {

    val c1 = CompteCourant(1, 1000.0, 500.0)
    val c2 = CompteEpargne(2, 2000.0, 5.0)
    val c3 = CompteCourant(3, 0.0, 200.0)
    val c4 = CompteEpargne(4, 0.0, 3.0)

    val comptes: MutableList<Compte> = mutableListOf()

    comptes.add(c1)
    comptes.add(c2)
    comptes.add(c3)
    comptes.add(c4)

    println("les comptes crees")

    println("le compte 1 est un compte courant avec un solde de " + c1.solde)
    println("le compte 2 est un compte epargne avec un solde de " + c2.solde)
    println("le compte 3 est un compte courant avec un solde de " + c3.solde)
    println("le compte 4 est un compte epargne avec un solde de " + c4.solde)

    println()

    println("operations de base")

    println("on verse 500 sur le compte 1")
    c1.verser(500.0)

    println("On retire 200 du compte 1")
    c1.retirer(200.0)

    println("on fait un virement de 300 du compte 1 vers le compte 2")
    c1.versement(300.0, c2)

    println("on verse 100 sur le compte 2")
    c2.verser(100.0)

    println("on retire 500 du compte 2")
    c2.retirer(500.0)

    println("on retire 100 du compte 2")
    c2.retirer(100.0)

    println("le solde du compte 1 est : " + c1.consulterSolde())
    println("le solde du compte 2 est : " + c2.consulterSolde())

    println()

    println("test des exceptions")

    try {
        println("on essaie de retirer 5000 du compte 1")
        c1.retirer(5000.0)

    } catch (e: SoldeInsuffisantException) {
        println("erreur : " + e.message)
    }

    try {
        println("on essaie de retirer 10000 du compte 2")
        c2.retirer(10000.0)

    } catch (e: SoldeInsuffisantException) {
        println("erreur : " + e.message)
    }

    try {
        println("on essaie de verser -50 sur le compte 1")
        c1.verser(-50.0)

    } catch (e: MontantInvalideException) {
        println("erreur : " + e.message)
    }

    println()

    println("mise a jour du solde avec les interets")

    println("solde du compte 2 avant les interets : " + c2.consulterSolde())

    c2.updateSolde()

    println("solde du compte 2 apres les interets : " + c2.consulterSolde())

    println("on essaie la mise a jour sur le compte courant 1")

    c1.updateSolde()

    println()

    println("liste des operations du compte 1")

    for (op in c1.listeOperation()) {
        println("Operation numero " + op.numero + "montant =" + op.montant + "date = " + op.dateOperation)
    }

    println("total des versements du compte 1 : " + c1.totalVersement())
    println("total des retraits du compte 1 : " + c1.totalRetrait())

    println()

    println("changer le statut")

    c1.changerStatut(StatutDeCompte.SUSPENDED)
    c1.changerStatut(StatutDeCompte.BLOCKED)

    try {
        println("on essaie de mettre BLOCKED une deuxieme fois")
        c1.changerStatut(StatutDeCompte.BLOCKED)

    } catch (e: StatutInvalideException) {
        println("erreur : " + e.message)
    }

    println()

    println("nombre de comptes")

    var nbEpargne = 0

    for (compte in comptes) {
        if (compte is CompteEpargne) {
            nbEpargne = nbEpargne + 1
        }
    }

    println("le nombre total de comptes crees est : " + comptes.size)
    println("le nombre de comptes epargne crees est : " + nbEpargne)

    println()

    println("comptes dont le solde est egal a zero")

    for (compte in comptes) {
        if (compte.solde == 0.0) {
            println("ce compte a un solde egal a zero : " + compte.resume())
        }
    }

    println()

    println("comptes avec un solde superieur a 500")

    val soldeSuperieur = { compte: Compte, montant: Double ->
        compte.solde > montant
    }

    for (compte in comptes) {
        if (soldeSuperieur(compte, 500.0)) {
            println("solde superieur a 500 : " + compte.resume())
        }
    }

    println()

    println("solde superieur a 500 et statut CREATED")

    val soldeEtStatut = { compte: Compte, montant: Double, statut: StatutDeCompte ->
        compte.solde > montant && compte.statut == statut
    }

    for (compte in comptes) {
        if (soldeEtStatut(compte, 500.0, StatutDeCompte.CREATED)) {
            println("solde superieur a 500 et statut CREATED : " + compte.resume())
        }
    }

    println()

    println("compte qui a fait le plus de versements")

    var meilleurCompte = comptes[0]
    var maxVersements = -1

    for (compte in comptes) {

        var nbVersements = 0

        for (op in compte.listeOperation()) {
            if (op is Versement) {
                nbVersements = nbVersements + 1
            }
        }

        if (nbVersements > maxVersements) {
            maxVersements = nbVersements
            meilleurCompte = compte
        }
    }

    println("le compte numero " + meilleurCompte.code + " a fait le plus de versements : " + maxVersements)

    println()

    println("afficher les comptes d'un type")

    println("voici les comptes courants :")
    afficherComptesParType(comptes, "courant")

    println("voici les comptes epargne :")
    afficherComptesParType(comptes, "epargne")

    println()

    println("compte avec le plus de transactions")

    val nombreOperations = { compte: Compte ->
        compte.listeOperation().size
    }

    var compteActif = comptes[0]

    for (compte in comptes) {
        if (nombreOperations(compte) > nombreOperations(compteActif)) {
            compteActif = compte
        }
    }

    println("le compte le plus actif est : " + compteActif.resume())

    println()

    println("totaux pour le compte 2")

    println("total des versements du compte 2 : " + c2.totalVersement())
    println("total des retraits du compte 2 : " + c2.totalRetrait())

    println()

    println("resume avec la fonction d'extension")

    println(c1.resume())

    println()

    println("creation un compte avec apply")

    val c5 = creerCompteCourantAvecVersement(5, 300.0, 100.0, 150.0)

    println("le compte 5 est cree avec 300 et un premier versement de 150")
    println(c5.resume())
}
