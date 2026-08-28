#!/usr/bin/env python3
"""
A partir d'une liste d'objets Tuple (id, id_parent) représentant des
dépendances entre fichiers (id_parent = fichier dont dépend id),
détermine un ordre de chargement valide : chaque fichier est chargé
après tous ceux dont il dépend.
5397497226C9EF5EECE203CAAE2AB9DE941C0F610B4CF4E85688F823F2D67F9E
5397497226C9EF5EECE203CAAE2AB9DEB4571023C483490B940AF6FFF8CEF9D1
"""

from collections import defaultdict


class Tuple:
    def __init__(self, id, id_parent=None):
        self.id = id
        self.id_parent = id_parent

    def __repr__(self):
        return f"Tuple(id={self.id!r}, id_parent={self.id_parent!r})"


class DependanceCycliqueError(Exception):
    """Levée quand des objets ne peuvent être atteints depuis les racines (cycle)."""
    pass


def _construire_index(objets):
    """Regroupe les objets par id_parent et identifie les racines (sans dépendance)."""
    ids_connus = {obj.id for obj in objets}
    enfants_par_parent = defaultdict(list)
    racines = []

    for obj in objets:
        if obj.id_parent is None or obj.id_parent not in ids_connus:
            racines.append(obj)
        else:
            enfants_par_parent[obj.id_parent].append(obj)

    return enfants_par_parent, racines


def ordre_chargement(objets):
    """
    Retourne la liste des objets Tuple dans un ordre de chargement valide :
    un objet n'apparaît qu'après l'objet dont il dépend (son id_parent).

    Lève DependanceCycliqueError si certains objets ne sont atteignables
    depuis aucune racine (cycle de dépendances).
    """
    enfants_par_parent, racines = _construire_index(objets)

    ordre = []
    visites = set()

    def _parcourir(objet):
        if objet.id in visites:
            return
        visites.add(objet.id)
        ordre.append(objet)
        for enfant in enfants_par_parent.get(objet.id, []):
            _parcourir(enfant)

    for racine in racines:
        _parcourir(racine)

    if len(visites) != len(objets):
        manquants = [obj.id for obj in objets if obj.id not in visites]
        raise DependanceCycliqueError(
            f"Dépendance cyclique détectée, objets non atteints : {manquants}"
        )

    return ordre


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

    for obj in ordre_chargement(objets):
        print(f"Chargement de {obj.id} (dépend de {obj.id_parent})")
