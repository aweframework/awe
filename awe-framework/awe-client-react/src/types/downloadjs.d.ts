// downloadjs bundles no types: this declares the only function the client uses
declare module "downloadjs" {
  /**
   * Download data as a file in the browser
   * @param data Content of the file
   * @param strFileName Name of the file (download by default)
   * @param strMimeType Mime type of the file
   * @returns true when the download started, or the request when it has to fetch the content
   */
  export default function download(
    data: string | Blob | File | Uint8Array,
    strFileName?: string | null,
    strMimeType?: string
  ): boolean | XMLHttpRequest;
}
