#!/usr/bin/env python3
"""
Construction d'une arborescence à partir d'un fichier JSON contenant
une liste d'objets possédant chacun un champ "id" et un champ "id_parent".

Usage :
    python build_tree.py chemin/vers/fichier.json
    python build_tree.py chemin/vers/fichier.json --json   # sortie JSON de l'arbre
"""

import json
import sys
import argparse
from collections import defaultdict


class NoeudInconnuError(Exception):
    """Levée quand un id_parent référence un id qui n'existe pas dans les données."""
    pass


def charger_donnees(chemin_fichier):
    with open(chemin_fichier, "r", encoding="utf-8") as f:
        return json.load(f)


def construire_arborescence(objets, cle_id="id", cle_parent="id_parent"):
    """
    Construit l'arborescence à partir d'une liste d'objets plats.

    Retourne :
        - racines : liste des ids des objets sans parent (id_parent None/absent/non trouvé)
        - enfants_par_id : dict {id: [liste des objets enfants]}
        - objets_par_id : dict {id: objet}
    """
    objets_par_id = {}
    for obj in objets:
        if cle_id not in obj:
            raise ValueError(f"Objet sans champ '{cle_id}' : {obj}")
        objets_par_id[obj[cle_id]] = obj

    enfants_par_id = defaultdict(list)
    racines = []

    for obj in objets:
        id_parent = obj.get(cle_parent)
        if id_parent is None:
            racines.append(obj[cle_id])
        elif id_parent not in objets_par_id:
            # Le parent référencé n'existe pas : on considère l'objet comme racine
            # (à remplacer par "raise NoeudInconnuError(...)" si on préfère être strict)
            racines.append(obj[cle_id])
        else:
            enfants_par_id[id_parent].append(obj)

    return racines, enfants_par_id, objets_par_id


def arbre_en_dict(id_objet, enfants_par_id, objets_par_id, cle_id="id"):
    """Convertit récursivement l'arbre en structure de dict imbriquée (sérialisable en JSON)."""
    objet = dict(objets_par_id[id_objet])  # copie pour ne pas modifier l'original
    fils = enfants_par_id.get(id_objet, [])
    objet["enfants"] = [
        arbre_en_dict(f[cle_id], enfants_par_id, objets_par_id, cle_id) for f in fils
    ]
    return objet


def afficher_arbre(id_objet, enfants_par_id, objets_par_id, cle_id="id", prefixe="", dernier=True):
    """Affiche l'arbre en mode texte façon 'tree'."""
    connecteur = "└── " if dernier else "├── "
    print(prefixe + connecteur + str(id_objet))

    fils = enfants_par_id.get(id_objet, [])
    nouveau_prefixe = prefixe + ("    " if dernier else "│   ")

    for i, f in enumerate(fils):
        est_dernier = (i == len(fils) - 1)
        afficher_arbre(f[cle_id], enfants_par_id, objets_par_id, cle_id, nouveau_prefixe, est_dernier)


def main():
    parser = argparse.ArgumentParser(description="Construire une arborescence à partir d'un fichier JSON plat.")
    parser.add_argument("fichier_json", help="Chemin vers le fichier JSON d'entrée")
    parser.add_argument("--json", action="store_true", help="Afficher le résultat au format JSON au lieu du format texte")
    parser.add_argument("--id-field", default="id", help="Nom du champ id (par défaut : id)")
    parser.add_argument("--parent-field", default="id_parent", help="Nom du champ id_parent (par défaut : id_parent)")
    args = parser.parse_args()

    donnees = charger_donnees(args.fichier_json)

    racines, enfants_par_id, objets_par_id = construire_arborescence(
        donnees, cle_id=args.id_field, cle_parent=args.parent_field
    )

    if args.json:
        arbre = [
            arbre_en_dict(r, enfants_par_id, objets_par_id, cle_id=args.id_field)
            for r in racines
        ]
        print(json.dumps(arbre, ensure_ascii=False, indent=2))
    else:
        for i, r in enumerate(racines):
            afficher_arbre(
                r, enfants_par_id, objets_par_id,
                cle_id=args.id_field, dernier=(i == len(racines) - 1)
            )


if __name__ == "__main__":
    main()
