# 🐦 Flappy Bird — Primer Parcial
### Programación Gráfica | Java + LWJGL | OpenGL 3.3 Core Profile

---

## 👤 Integrante
| Nombre  |
|-----------------------|
| Oscar Cruz Rodriguez |

---

## 🎮 Controles

### En el Menú Principal
| Tecla | Acción |
|---|---|
| `↑` / `↓` | Navegar opciones (1 Jugador / 2 Jugadores) |
| `ENTER` | Confirmar selección e iniciar partida |
| `ESC` | Cerrar el juego |

### Durante la Partida
| Jugador | Tecla | Color |
|---|---|---|
| Jugador 1 | `ESPACIO` | 🟡 Amarillo |
| Jugador 2 | `W` o `↑` | 🔵 Cian |

### En Pantalla Game Over
| Tecla | Acción |
|---|---|
| `↑` / `↓` | Navegar entre "Reiniciar" y "Menú" |
| `ENTER` | Confirmar |

---

##  Compilación y Ejecución

### Requisitos
- **JDK 17** o superior
- **Apache Maven 3.6+**
- Sistema operativo **Windows** (nativos LWJGL configurados para Windows)

### Comando
```bash
mvn compile exec:java
```

---

##  Estructura del Proyecto

```
src/main/java/com/graphics/flappy/
├── FlappyGame.java     # Bucle principal, renderizado, menús, HUD
├── Bird.java           # Entidad pájaro: física, animación, renderizado compuesto
├── PipeManager.java    # Generación, movimiento, colisiones y puntuación de tubos
├── SoundEngine.java    # Motor de audio (OpenAL, sonidos procedurales)
└── Shader.java         # Compilación y gestión de shaders GLSL
```

---

##  Descripción de los Cambios Implementados

### 2.1 — Pájaro Compuesto por Figuras Geométricas

El rectángulo original fue sustituido por un personaje construido íntegramente con primitivas OpenGL:

| Parte | Implementación |
|---|---|
| **Cuerpo** | Matriz de píxeles 10×10 renderizada con quads (`VAO` de rectángulo) |
| **Pico** | Triángulo real con su propio `VAO` (`vaoTri`), alineado matemáticamente al cuerpo |
| **Ojo + Pupila** | Dos rectángulos concéntricos definidos en la matriz de sprites |
| **Ala** | Animada cíclicamente con un `wingTimer`, sincronizada con el salto |
| **Cola** | Incluida en la parte posterior de la matriz de sprites |
| **Inclinación** | Sistema `tilt/shear`: Java calcula el ángulo según `velY` y lo pasa al vertex shader via uniform `uT` |

El vertex shader aplica la deformación: `gl_Position.y += pos.x * scale.x * uT`, logrando que el pico y el cuerpo se inclinen como una sola unidad sólida.

---

### 2.2 — Modo Dos Jugadores Simultáneos

- Dos pájaros independientes creados en `reset()` / `startGame(numPlayers)`.
- Cada pájaro tiene su propio `x`, `y`, `velY`, `alive` y `score`.
- **Menú de inicio** con selección de 1 o 2 jugadores mediante flechas + Enter.
- Al morir, el pájaro cae por gravedad (el update continúa aunque `alive = false`) y desaparece al salir de la pantalla (`y < -1.2f` no se dibuja).
- El puntaje queda **bloqueado** al morir: `if (b.alive) b.score++` en `PipeManager`.
- La partida termina únicamente cuando **ambos** pájaros han muerto.
- Los puntajes se muestran en pantalla con la fuente pixel-art en el color de cada pájaro.

---

### 2.3 — Incremento Progresivo de Dificultad

La dificultad se recalcula en cada frame en `PipeManager.update()`:

```java
// Cada 5 puntos = +1 nivel
currentSpeed    = Math.min(1.8f, 0.6f + nivel * 0.15f);  // tope: 1.8
currentInterval = Math.max(0.8f, 1.6f - nivel * 0.10f);  // mínimo: 0.8 s
```

| Nivel | Velocidad tubos | Intervalo de aparición |
|---|---|---|
| 1 | 0.60 | 1.60 s |
| 2 | 0.75 | 1.50 s |
| 3 | 0.90 | 1.40 s |
| … | … | … |
| 9+ | **1.80 (tope)** | **0.80 s (tope)** |

El nivel actual se muestra en el HUD central en blanco.

---

### 2.4 — Mejora de la Interfaz

#### Fondo Dinámico
- **Cielo**: Color sólido azul claro.
- **Nubes**: 5 nubes pixel-art construidas con rectángulos, cada una con velocidad distinta, en bucle infinito.
- **Montañas (Parallax)**: 6 montañas en 2 capas de profundidad. La capa trasera se mueve a 0.04 u/s y la delantera a 0.08 u/s, creando efecto de profundidad. Cada montaña tiene escalones con nieve en la cima y franjas de sombra interna.
- **Suelo**: Rectángulo verde con borde oscuro.

#### Tubos Mejorados
- Tapa (`cap`) más ancha que el cuerpo en el extremo abierto.
- Borde negro en todos los lados (escalado + desplazamiento del rectángulo oscuro).
- Franja de brillo verde claro en el lateral izquierdo.

#### HUD Pixel-Art
- Fuente 3×5 personalizada dibujada con rectángulos individuales.
- Soporta dígitos `0–9` y letras `A B D E F G I J L M N O P R S U V Y C`.
- Puntaje P1 en la esquina izquierda (amarillo), P2 a la derecha (cian), nivel en el centro (blanco).

#### Pantallas
- **Menú Principal**: Título "FLAPPY BIRD" grande + "1 JUGADOR" / "2 JUGADORES" con cursor triangular navegable.
- **Game Over**: Texto "GAME OVER" en rojo + opciones "REINICIAR" / "MENU" con cursor triangular rojo.

#### Sistema de Sonido (OpenAL — 100% Procedural)
Todos los sonidos se generan matemáticamente al iniciar el juego. No se usan archivos de audio externos.

| Evento | Descripción del Sonido |
|---|---|
| Salto P1 🟡 | Chirp ascendente 300→700 Hz (mezcla square+sine) |
| Salto P2 🔵 | Chirp ascendente 400→900 Hz (tono más agudo) |
| Puntaje ✨ | Dos notas Mi5→Sol5, estilo moneda retro |
| Muerte P1  | Glissando 580→60 Hz con vibrato creciente |
| Muerte P2  | Glissando 480→50 Hz (tono levemente distinto) |
| Subida de nivel  | Arpegio ascendente C5→E5→G5→C6 |
| Selección de menú | Click corto 320 Hz |
| Confirmar (Enter) | Acorde Do Mayor (C5+E5+G5) |

---

##  Flujo del Bucle Principal

```
loop():
  ├── Actualizar nubes y montañas (siempre, incluso en menú)
  ├── input()  →  menú / juego / game over
  ├── if PLAYING:
  │   ├── Capturar prevScores[] y wasAlive[]
  │   ├── pipeManager.update()  →  colisiones, score, spawn
  │   ├── bird.update()         →  gravedad, muerte por suelo
  │   ├── Detectar muertes  →  playDie(i)
  │   ├── Detectar scores   →  playScore()
  │   └── Detectar nivel    →  playLevelUp()
  └── render()
      ├── Fondo (montañas, nubes, suelo)
      ├── if START   → drawText menú
      ├── if PLAYING → pipeManager.render + birds.render + HUD
      └── if GAMEOVER→ escena congelada + menú game over
```
