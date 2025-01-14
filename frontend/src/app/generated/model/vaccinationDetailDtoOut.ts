/**
 * Mild Blue - Covid Vaxx
 * Covid Vaxx API
 *
 * The version of the OpenAPI document: development
 * Contact: support@mild.blue
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
import { PersonnelDtoOut } from './personnelDtoOut';


export interface VaccinationDetailDtoOut {
  bodyPart: VaccinationDetailDtoOutBodyPartEnum;
  doctor: PersonnelDtoOut;
  exportedToIsinOn?: string | null;
  notes?: string | null;
  nurse?: PersonnelDtoOut;
  patientId: string;
  vaccinatedOn: string;
  vaccinationId: string;
  vaccineExpiration?: string | null;
  vaccineSerialNumber: string;
}

export enum VaccinationDetailDtoOutBodyPartEnum {
  DominantHand = 'DOMINANT_HAND',
  NonDominantHand = 'NON_DOMINANT_HAND',
  Buttock = 'BUTTOCK'
};



