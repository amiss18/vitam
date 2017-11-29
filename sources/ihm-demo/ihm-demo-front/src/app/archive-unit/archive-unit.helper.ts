import { Injectable } from '@angular/core';
import {DatePipe} from "@angular/common";
import {SelectItem} from "primeng/primeng";
import {ObjectsService} from "../common/utils/objects.service";

@Injectable()
export class ArchiveUnitHelper {
  private mustExclude = ['#id', 'StartDate', 'EndDate', 'Title', 'DescriptionLevel', 'Description',
    ['#management.SubmissionAgency'], ['#management.OriginatingAgency'], 'inheritedRule'];
  public rulesCategories = [
    {rule: 'AccessRule', label: 'Délai de communicabilité'},
    {rule: 'AppraisalRule', label: 'Durée d\'utilité administrative'},
    {rule: 'ClassificationRule', label: 'Durée de classification'},
    {rule: 'DisseminationRule', label: 'Délai de diffusion'},
    {rule: 'ReuseRule', label: 'Durée de réutilisation'},
    {rule: 'StorageRule', label: 'Durée d\'utilité courante'}
  ];
  public storageFinalAction = {
    RestrictAccess: {id: 'RestrictAccess', label: 'Accès Restreint'},
    Transfer: {id: 'Transfer', label: 'Transférer'},
    Copy: {id: 'Copy', label: 'Copier'}
  };
  public appraisalFinalAction = {
    Keep: {id: 'Keep', label: 'Conserver'},
    Destroy: {id: 'Destroy', label: 'Détruire'}
  };
  public textAreaFields = [
    'Description',
    'CustodialHistory.CustodialHistoryItem'
  ];
  public selectionFields = [
    'DescriptionLevel',
    '#unitType'
  ];
  public selectionOptions = {
    'DescriptionLevel': [
      {label: 'Fonds', value: 'Fonds'},
      {label: 'Subfonds', value: 'Subfonds'},
      {label: 'Class', value: 'Class'},
      {label: 'Collection', value: 'Collection'},
      {label: 'Series', value: 'Series'},
      {label: 'Subseries', value: 'Subseries'},
      {label: 'RecordGrp', value: 'RecordGrp'},
      {label: 'SubGrp', value: 'SubGrp'},
      {label: 'File', value: 'File'},
      {label: 'Item', value: 'Item'}
    ],
    '#unitType': [
      {label: 'Standard', value: 'INGEST'},
      {label: 'Plan de classement', value: 'FILING_UNIT'},
      {label: 'Arbre de positionnement', value: 'HOLDING_UNIT'}
    ]
  };

  constructor() { }

  mustExcludeFields(field: string) {
    return this.mustExclude.indexOf(field) !== -1 || field.startsWith('_') || field.startsWith('#');
  }

  transformType(unitType: string) {
    switch (unitType) {
      case 'INGEST': return 'Standard';
      case 'FILING_UNIT': return 'Plan de classement';
      case 'HOLDING_UNIT': return 'Arbre de positionnement';
      default: return unitType;
    }
  }

  getStartDate(unitData: any) {
    if (unitData.DescriptionLevel !== 'Item') {
      return unitData.StartDate;
    }
    let lowestDate = '';
    if (unitData.CreatedDate && (lowestDate === '' || lowestDate > unitData.CreatedDate)) {
      lowestDate = unitData.CreatedDate;
    }
    if (unitData.AcquiredDate && (lowestDate === '' || lowestDate > unitData.AcquiredDate)) {
      lowestDate = unitData.AcquiredDate;
    }
    if (unitData.SentDate && (lowestDate === '' || lowestDate > unitData.SentDate)) {
      lowestDate = unitData.SentDate;
    }
    if (unitData.ReceivedDate && (lowestDate === '' || lowestDate > unitData.ReceivedDate)) {
      lowestDate = unitData.ReceivedDate;
    }
    if (unitData.RegisteredDate && (lowestDate === '' || lowestDate > unitData.RegisteredDate)) {
      lowestDate = unitData.RegisteredDate;
    }
    if (unitData.TransactedDate && (lowestDate === '' || lowestDate > unitData.TransactedDate)) {
      lowestDate = unitData.TransactedDate;
    }
    return lowestDate;
  }

  getEndDate(unitData: any) {
    if (unitData.DescriptionLevel !== 'Item') {
      return unitData.EndDate;
    }
    let lowestDate = '';
    if (unitData.CreatedDate && (lowestDate === '' || lowestDate < unitData.CreatedDate)) {
      lowestDate = unitData.CreatedDate;
    }
    if (unitData.AcquiredDate && (lowestDate === '' || lowestDate < unitData.AcquiredDate)) {
      lowestDate = unitData.AcquiredDate;
    }
    if (unitData.SentDate && (lowestDate === '' || lowestDate < unitData.SentDate)) {
      lowestDate = unitData.SentDate;
    }
    if (unitData.ReceivedDate && (lowestDate === '' || lowestDate < unitData.ReceivedDate)) {
      lowestDate = unitData.ReceivedDate;
    }
    if (unitData.RegisteredDate && (lowestDate === '' || lowestDate < unitData.RegisteredDate)) {
      lowestDate = unitData.RegisteredDate;
    }
    if (unitData.TransactedDate && (lowestDate === '' || lowestDate < unitData.TransactedDate)) {
      lowestDate = unitData.TransactedDate;
    }
    return lowestDate;
  }

  isTextArea(field: string): boolean {
    return this.textAreaFields.indexOf(field) !== -1;
  }

  isSelection(field: string): boolean {
    return this.selectionFields.indexOf(field) !== -1;
  }

  getOptions(field: string): any[] {
    return this.selectionOptions[field];
  }

  isText(field: string): boolean {
    return !this.isTextArea(field) && !this.isSelection(field);
  }

  personOrEntityGroup = {
    '@@': 'Entité',
    'CorpName': 'Nom de l\'entité',
    'Gender': 'Sexe',
    'Nationality': 'Nationalité',
    'BirthDate': 'Date de naissance',
    'DeathDate': 'Date de décès',
    'Identifier': 'Identifiant',
    'BirthName': 'Nom de naissance',
    'FirstName': 'Prénom',
    'GivenName': 'Nom d\'Usage',
    'Function': 'Fonction',
    'Activity': 'Activité',
    'Role': 'Droits',
    'Position': 'Intitulé du poste de travail',
    'BirthPlace': {
      '@@': 'Lieu de naissance',
      'Geogname': 'Nom géographique',
      'Address': 'Adresse',
      'PostalCode': 'Code postal',
      'City': 'Ville',
      'Region': 'Région',
      'Country': 'Pays'
    },
    'DeathPlace':{
      '@@': 'Lieu de décès',
      'Geogname': 'Nom géographique',
      'Address': 'Adresse',
      'PostalCode': 'Code postal',
      'City': 'Ville',
      'Region': 'Région',
      'Country': 'Pays'
    }
  };

  multiLang = {
    '@@': 'Champ',
    'fr': 'Français',
    'fre': 'Français',
    'en': 'Anglais',
    'eng': 'Anglais',
    'de': 'Allemand',
    'sp': 'Espagnol',
    'it': 'Italien',
  };

  getPersonOrEntityGroup(entityName: string) {
    let entity = ObjectsService.clone(this.personOrEntityGroup);
    entity['@@'] = entityName;
    return entity;
  }

  getFieldWithLang(fieldName: string) {
    let field = ObjectsService.clone(this.multiLang);
    field['@@'] = fieldName;
    return field;
  }

  getTranslationConstants() {
    return {
      'DescriptionLevel': 'Niveau de description',
      'Title': 'Titre',
      'FilePlanPosition': 'Position dans le plan de classement',
      'ID': 'Id',
      'OriginatingSystemId': 'Id système d\'origine',
      'SystemId': 'Identifiant système',
      'ArchivalAgencyArchiveUnitIdentifier': 'Id métier (Service d\'archives)',
      'OriginatingAgencyArchiveUnitIdentifier': 'Id métier (Service producteur)',
      'TransferringAgencyArchiveUnitIdentifier': 'Id métier (Serivce versant)',
      'Description': 'Description',
      'CustodialHistory': {
        '@@': 'Historique',
        'CustodialHistoryItem': 'Historique de propriété, de responsabilité et de conservation'
      },
      'Type': 'Type d\'information (Sens OAIS)',
      'DocumentType': 'Type de document',
      'Language': 'Langue des documents',
      'DescriptionLanguage': 'Langue des descriptions',
      'Status': 'Etat de l\'objet',
      'Version': 'Version',
      'Tag': 'Mot-clés',
      'Keyword': {
        '@@': 'Mot-clés',
        'KeywordContent': 'Valeur du mot-clé',
        'KeywordType': 'Type de mot-clé',
        'KeywordReference': 'Identifiant du mot clé',
      },
      'Coverage': {
        '@@': 'Autres métadonnées de couverture',
        'Spatial': 'Couverture géographique',
        'Temporal': 'Couverture temporelle',
        'Juridictional': 'Couverture administrative'
      },
      'OriginatingAgency': {
        '@@': 'Service producteur',
        'Identifier': 'Id Service producteur',
        'OrganizationDescriptiveMetadata': 'Nom du service producteur'
      },
      'SubmissionAgency': {
        '@@': 'Service versant',
        'Identifier': 'Id Service versant',
        'OrganizationDescriptiveMetadata': 'Nom du service versant'
      },
      'AuthorizedAgent': this.getPersonOrEntityGroup('Titulaire des droits de propriété intellectuelle'),
      'AuthorizedAgentGroup': {
        '@@': 'Titulaire(s) des droits de propriété intellectuelle',
        'AuthorizedAgent': this.getPersonOrEntityGroup('Titulaire des droits de propriété intellectuelle')
      },
      'Writer': this.getPersonOrEntityGroup('Rédacteur'),
      'WritingGroup': {
        '@@': 'Rédacteur(s)',
        'Writer': this.getPersonOrEntityGroup('Rédacteur'),
      },
      'Addressee': this.getPersonOrEntityGroup('Destinataire'),
      'Recipient': this.getPersonOrEntityGroup('Destinataire'),
      'AudienceGroup': {
        '@@': 'Audience(s) du document',
        'Addressee': this.getPersonOrEntityGroup('Destinataire'),
        'Recipient': this.getPersonOrEntityGroup('Destinataire'),
      },
      'AddresseeGroup': {
        '@@': 'Destinataire(s) pour action',
        'Addressee': this.getPersonOrEntityGroup('Destinataire'),
      },
      'RecipientGroup': {
        '@@': 'Destinataire(s) pour information',
        'Recipient': this.getPersonOrEntityGroup('Destinataire'),
      },
      'SignerGroup': {
        '@@': 'Signataire(s) ',
        'Signer': this.getPersonOrEntityGroup('Signataire'),
      },
      'ValidationGroup': {
        '@@': 'Validateur(s) de la signature',
        'Validator': this.getPersonOrEntityGroup('Validateur de la signature'),
      },

      'Source': 'Référence papier',
      'RelatedObjectReference': 'Référence à un objet',
      'CreatedDate': 'Date de création',
      'TransactedDate': 'Date de transaction',
      'AcquiredDate': 'Date de numérisation',
      'SentDate': 'Date d\'envoi',
      '#originating_agency': 'Service producteur de l\'entrée',
      '#originating_agencies': 'Service ayant des droits  sur l\'unité',
      'ReceivedDate': 'Date de reception',
      'RegisteredDate': 'Date d\'enregistrement',
      'StartDate': 'Date de début',
      'EndDate': 'Date de fin',
      'Event': {
        '@@': 'Evénement',
        'EventDateTime': 'Date et heure de l\'événement',
        'EventIdentifier': 'Identifiant de l\'événement',
        'EventType': 'Type d\'événement',
        'EventDetail': 'Détail de l\'événement'
      },
      'ArchiveUnitProfile': 'Profil d\'archivage',
      '#mgt': {
        'NeedAuthorization': 'Autorisation requise',
      },
      'Titles': this.getFieldWithLang('Titres'),
      'Descriptions': this.getFieldWithLang('Descriptions'),
      'Gps': {
        '@@': 'Coordonnées GPS',
        'GpsLatitude': 'Latitude',
        'GpsLongitude': 'Longitude',
        'GpsAltitude': 'Altitude'
      },
      'StorageRule.Rule': 'Règle d\'utilité courante (DUC)',
      'StorageRule.FinalAction': 'Action finale',
      'AppraisalRule.Rule': 'Règle d\'utilité administrative (DUA)',
      'AppraisalRule.FinalAction': 'Action finale',
      'AccessRule.Rule': 'Règle de communicabilité',
      'AccessRule.FinalAction': 'Action finale',
      'DisseminationRule.Rule': 'Règle de diffusion',
      'DisseminationRule.FinalAction': 'Action finale',
      'ReuseRule.Rule': 'Règle de réutilisation',
      'ReuseRule.FinalAction': 'Action finale',
      'ClassificationRule.Rule': 'Règle de classification',
      'ClassificationRule.FinalAction': 'Action finale',
      'ClassificationRule.ClassificationLevel': 'Niveau de classification',
      'ClassificationRule.ClassificationOwner': 'Émetteur de la classification'
    };
  }

  getObjectGroupTranslations() {
    return {
      '_id': 'Identifiant',
      'DataObjectGroupId': 'Identifiant du groupe d\'objets techniques',
      'DataObjectVersion': 'Usage',
      'MessageDigest': 'Empreinte',
      'OtherMetadata': 'Autres métadonnées',
      'Size': 'Taille (en octets)',
      'Algorithm': 'Algorithme',
      'FormatIdentification': 'Format',
      'FormatIdentification.FormatLitteral': 'Nom littéral',
      'FormatIdentification.MimeType': 'Type Mime',
      'FormatIdentification.FormatId': 'PUID du format',
      'FileInfo': 'Fichier',
      'FileInfo.Filename': 'Nom du fichier',
      'FileInfo.CreatingApplicationName': 'Nom de l\'application utilisée pour créer le fichier',
      'FileInfo.DateCreatedByApplication': 'Date de création par l\'application',
      'FileInfo.CreatingApplicationVersion': 'Version de l\'application utilisée pour créer le fichier',
      'FileInfo.CreatingOs': 'Système d\'exploitation utilisé pour créer le fichier',
      'FileInfo.CreatingOsVersion': 'Version du système d\'exploitation utilisé pour créer le fichier',
      'FileInfo.LastModified': 'Date de dernière modification',
      'FormatIdentification.Encoding': 'Encodage',
      'Metadata': 'Métadonnées',
      'Metadata.OtherMetadata': 'Autres métadonnées',
      'Metadata.MyOtherCoreTechnicalMetadataAbstract': 'Autre objet',
      'Metadata.Audio': 'Audio',
      'Metadata.Image': 'Image',
      'Metadata.Text':'Texte',
      '_storage': 'Stockage',
      '_storage._nbc': 'Nombre de copies',
      '_storage.offerIds': 'offre de stockage',
      '_storage.strategyId': 'Stratégie de stockage',
      'PhysicalId': 'Identifiant d\'objet physique',
      'PhysicalDimensions': 'Dimensions physiques de l\'objet',
      'PhysicalDimensions.Shape': 'Forme',
      'PhysicalDimensions.NumberOfPage': 'Nombre de pages',
      'PhysicalDimensions.Width': 'Largeur',
      'PhysicalDimensions.Width.unit':'Unité',
      'PhysicalDimensions.Width.value':'Valeur',
      'PhysicalDimensions.Height': 'Hauteur',
      'PhysicalDimensions.Height.unit':'Unité',
      'PhysicalDimensions.Height.value':'Valeur',
      'PhysicalDimensions.Depth': 'Profondeur',
      'PhysicalDimensions.Depth.unit':'Unité',
      'PhysicalDimensions.Depth.value':'Valeur',
      'PhysicalDimensions.Diameter': 'Diamètre',
      'PhysicalDimensions.Diameter.unit':'Unité',
      'PhysicalDimensions.Diameter.value':'Valeur',
      'PhysicalDimensions.Length': 'Longueur',
      'PhysicalDimensions.Length.unit':'Unité',
      'PhysicalDimensions.Length.value':'Valeur',
      'PhysicalDimensions.Thickness': 'Epaisseur',
      'PhysicalDimensions.Thickness.unit':'Unité',
      'PhysicalDimensions.Thickness.value':'Valeur',
      'PhysicalDimensions.Weight': 'Poids',
      'PhysicalDimensions.Weight.unit':'Unité',
      'PhysicalDimensions.Weight.value':'Valeur'
    };
  }
}
