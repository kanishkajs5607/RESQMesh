# Real prototype screenshots

Upload the team's actual images to this folder. No screenshots are currently included.

| Filename | What the image should show |
|---|---|
| `01-citizen-mode.png` | Citizen Mode home screen |
| `02-mesh-active.png` | OFFLINE MESH ACTIVE and nearby-node discovery |
| `03-multi-device-network.png` | Multiple connected devices / node list |
| `04-create-sos.png` | Create SOS form |
| `05-rescuer-mode-relayed-sos.png` | Rescuer Mode, received CRITICAL SOS and the visible 2-hop relay path |
| `06-physical-device-demo.png` | The real photo of two physical phones running RESQMesh; caption it as two phones, not all three |

If the CRITICAL card and relay path do not fit one genuine screenshot, add `07-two-hop-relay-path.png`. An additional discovery screenshot can use `08-nearby-node-discovery.png`.

## Upload and display

1. Open this folder on GitHub and choose **Add file → Upload files**.
2. Upload the real PNG files under the exact names above and commit to main. If your originals are JPEG, export actual PNGs or use `.jpg` filenames and update the Markdown accordingly; renaming an extension does not convert an image.
3. Replace the root README's “Screenshots / Prototype Proof” placeholder paragraphs with the block below **only after all six files exist**. Its paths are relative to the root README.

```markdown
| Citizen Mode | Mesh active / discovery | Connected devices |
|---|---|---|
| ![Citizen Mode](docs/screenshots/01-citizen-mode.png) | ![Offline mesh and nearby discovery](docs/screenshots/02-mesh-active.png) | ![Multiple connected nodes](docs/screenshots/03-multi-device-network.png) |

| Create SOS | Rescuer receives a relayed SOS | Physical prototype |
|---|---|---|
| ![SOS creation](docs/screenshots/04-create-sos.png) | ![Critical SOS and two-hop path](docs/screenshots/05-rescuer-mode-relayed-sos.png) | ![Two physical phones running RESQMesh](docs/screenshots/06-physical-device-demo.png) |
```

Preserve visible message IDs, origin and hop/path details where possible. Use only demo information or images suitable for public sharing. Do not alter the emergency/path details to imply a result absent from the original screen.
