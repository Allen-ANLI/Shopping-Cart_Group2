import fs from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const workspaceDir = 'D:/DOCUMENTS/GDipSA/CA_Web Application/Group2';
const buildDir = path.join(workspaceDir, '.ppt-build');
const runtimeModules = 'C:/Users/12458/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules';
const skillDir = 'C:/Users/12458/.codex/plugins/cache/openai-primary-runtime/presentations/26.1013.11914/skills/presentations';
const runtimePython = 'C:/Users/12458/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe';
process.env.RUNTIME_NODE_MODULES = runtimeModules;
const { Presentation, PresentationFile, FileBlob } = await import(pathToFileURL(path.join(runtimeModules, '@oai/artifact-tool/dist/artifact_tool.mjs')).href);
const { finalizePresentation } = await import(pathToFileURL(path.join(skillDir, 'container_tools/artifact_tool_utils.mjs')).href);

const width = 1672;
const height = 941;
const images = [
  ['01-feature-design-en.png', 'Feature Design — Core Features and Enhancements'],
  ['02-architecture-en.png', 'System Architecture and Technology Stack'],
  ['03-cloud-deployment-en.png', 'Cloud Deployment and Delivery Pipeline'],
];
const presentation = Presentation.create({ slideSize: { width, height } });
for (const [file, title] of images) {
  const sourcePath = path.join(workspaceDir, 'defense-images/english', file);
  const slide = presentation.slides.add();
  slide.images.add({
    blob: new Uint8Array(await fs.readFile(sourcePath)),
    contentType: 'image/png',
    alt: title,
    fit: 'contain',
    position: { left: 0, top: 0, width, height },
  });
  slide.speakerNotes.text = `Source: ${sourcePath}\nOriginal English presentation image created for the Group2 project defense. Included unchanged at its original aspect ratio.`;
}

const candidatePath = path.join(buildDir, 'candidate.pptx');
const finalPath = path.join(workspaceDir, 'defense-presentation/Group2-Defense-English.pptx');
await (await PresentationFile.exportPptx(presentation)).save(candidatePath);
const result = await finalizePresentation({
  explicitTotalSlideCount: 3,
  requiredNativeTableOwnerSlides: [],
  requiredNativeChartOwnerSlides: [],
  workspaceDir,
  candidatePath,
  finalPath,
  pythonExecutable: runtimePython,
  integrityValidatorPath: path.join(skillDir, 'container_tools/inspect_presentation_package_integrity.py'),
  layoutValidatorPath: path.join(skillDir, 'container_tools/inspect_presentation_layout_geometry.py'),
  layoutArgs: [
    '--expected-slide-size-emu', `${width * 9525},${height * 9525}`,
    '--validate-bullet-geometry',
    '--validate-heading-fit',
  ],
  verifyArtifactToolImport: true,
  receiptPath: path.join(buildDir, 'Group2-Defense-English.pptx.validation.json'),
});
console.log(JSON.stringify(result));

const finalPresentation = await PresentationFile.importPptx(await FileBlob.load(finalPath));
for (let i = 0; i < 3; i++) {
  const slide = finalPresentation.slides.items[i];
  const image = await finalPresentation.export({ slide, format: 'png', scale: 1 });
  await fs.writeFile(path.join(buildDir, `slide-${i + 1}.png`), new Uint8Array(await image.arrayBuffer()));
}
console.log('Rendered all three final slides.');
