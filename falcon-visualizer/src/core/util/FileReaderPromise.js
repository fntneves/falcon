export default class FileReaderPromise {
  constructor(file) {
    this.file = file;
  }

  readAsText() {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = e => resolve(e.target.result);
      reader.onerror = e => reject(new Error(`Error reading ${this.file.name}: ${e.target.result}`));
      reader.readAsText(this.file);
    });
  }
}
