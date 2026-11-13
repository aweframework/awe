// validate-date bundles no types: this declares the only call the client makes, which asks for a boolean answer
declare module "validate-date" {
  /**
   * Check a date against a format
   * @param dateValue Date to check
   * @param responseType Kind of answer: a boolean
   * @param dateFormat Format of the date, like "dd/mm/yyyy"
   * @returns true when the date is valid
   */
  export default function validateDate(dateValue: string, responseType: "boolean", dateFormat?: string | null): boolean;
}
