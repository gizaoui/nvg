#!/usr/bin/env python3
"""
Affichage direct d'une arborescence à partir d'une liste d'objets Tuple
(chacun possédant un attribut "id" et un attribut "id_parent"),
déjà présente en mémoire.
"""

from collections import defaultdict


class Tuple:
    def __init__(self, id, id_parent=None):
        self.id = id
        self.id_parent = id_parent

    def __repr__(self):
        return f"Tuple(id={self.id!r}, id_parent={self.id_parent!r})"


def afficher_arborescence(objets):
    """
    Affiche l'arborescence complète à partir d'une liste plate d'objets Tuple.
    Gère plusieurs racines (id_parent None ou id_parent inconnu de la liste).
    """
    ids_connus = {obj.id for obj in objets}
    enfants_par_parent = defaultdict(list)
    racines = []

    for obj in objets:
        if obj.id_parent is None or obj.id_parent not in ids_connus:
            racines.append(obj)
        else:
            enfants_par_parent[obj.id_parent].append(obj)

    def _afficher(objet, prefixe="", dernier=True):
        connecteur = "└── " if dernier else "├── "
        print(prefixe + connecteur + str(objet.id))

        fils = enfants_par_parent.get(objet.id, [])
        nouveau_prefixe = prefixe + ("    " if dernier else "│   ")
        for i, enfant in enumerate(fils):
            _afficher(enfant, nouveau_prefixe, dernier=(i == len(fils) - 1))

    for i, racine in enumerate(racines):
        _afficher(racine, dernier=(i == len(racines) - 1))


if __name__ == "__main__":
    objets = [
        Tuple(1),
        Tuple(2, 1),
        Tuple(3, 1),
        Tuple(4, 2),
        Tuple(5, 2),
        Tuple(6),
        Tuple(7, 6),
    ]
    afficher_arborescence(objets)
