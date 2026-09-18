# Google Play — Ficha de la tienda en español

## Nombre de la app (máx. 30 caracteres)

```
Sudomnia
```

## Descripción breve (máx. 80 caracteres)

```
Sudoku sin adivinar. Sin anuncios, sin cuenta, ni un solo permiso.
```

## Descripción completa (máx. 4000 caracteres)

```
Sudomnia es un sudoku para quienes quieren resolver el rompecabezas, no pelearse con la app.

Cada sudoku se genera en tu dispositivo y se comprueba antes de que lo veas: tiene exactamente una solución, y esa solución se alcanza solo razonando.


NINGÚN SUDOKU REQUIERE ADIVINAR

Esta es la promesa sobre la que se construye toda la app, y se comprueba, no se afirma sin más. Antes de que se te muestre un sudoku, Sudomnia lo resuelve como lo haría una persona — únicos ocultos y desnudos, candidatos bloqueados, pares y tríos desnudos y ocultos, X-Wing, pez espada, coloreado simple, XY-Wing — y nunca probando una cifra a ver qué pasa. Si no se puede terminar así, no se te ofrece.

La mayoría de los generadores no hacen esto. Comprueban que la solución es única y ahí se quedan. Medimos nuestra propia versión anterior: de los sudokus que llamaba «difíciles», el 53 % no se podían terminar con ninguna de esas técnicas. Si te quedabas atascado en uno, no había forma de saber si se te escapaba algo o si no había nada que ver.


CUATRO NIVELES QUE SIGNIFICAN ALGO

El nivel no es una suposición según cuántas cifras quedan dadas. Es la técnica más difícil que ese sudoku en concreto exige de verdad, medida sobre esa misma cuadrícula:

• Fácil — solo únicos, y quedan al menos 36 cifras dadas
• Medio — solo únicos, pero excavado tan a fondo como permite la unicidad
• Difícil — candidatos bloqueados, o un par o trío
• Experto — un X-Wing, coloreado o un XY-Wing

El número de cifras dadas por sí solo no dice nada: los sudokus fáciles excavados al máximo y los que requieren técnicas de verdad acaban ambos alrededor de 24 cifras dadas.


PISTAS QUE SE EXPLICAN SOLAS

La pista no revela una cifra y te deja igual de perdido que antes. Nombra la técnica y el motivo: «Candidatos bloqueados: en la caja 5 el 7 solo cabe en celdas que también están en la columna 3, así que puede eliminarse del resto de la columna 3». Lo que tacha aparece tachado en el propio tablero. Cuando hacen falta varios pasos antes de poder escribir una cifra, te los muestra uno por uno.

La última pulsación coloca la cifra — pero para entonces ya sabes por qué va ahí.


GRATIS. SIN ANUNCIOS. SIN CUENTA. SIN RASTREO.

Ninguno lleva un asterisco.

La app no tiene ni un solo permiso de Android que le permita hacer nada — ni siquiera acceso a internet. No puede enviar nada a ningún sitio, porque no tiene con qué. Tus partidas nunca salen del dispositivo. Comprobable: Sudomnia es de código abierto.

No hay ningún servidor detrás. Los sudokus se crean en tu dispositivo, en pocos milisegundos cada uno.


LAS AYUDAS SON TUYAS PARA DESACTIVARLAS

Marcar conflictos, resaltar la misma cifra, resaltar fila/columna/caja, atenuar cifras completas: cada una por separado. La primera es la importante — te dice al instante si una cifra es siquiera posible, y eso es la mitad del razonamiento. Desactívala y la app se queda callada.

Una partida resuelta con todas las ayudas desactivadas gana una insignia en las estadísticas. Desactivarlas justo antes de la última celda no gana nada.


PRUEBA UNA RAMA EN LUGAR DE ADIVINAR

Cuando quieras suponer una cifra y seguirle la pista, «Iniciar rama» marca ese punto. Todo lo que introduzcas después es provisional y aparece en amarillo. Si sale bien, lo conservas; si no, descartas todo el intento de un paso — incluidas las notas que fue borrando por el camino.


PENSADA PARA QUEDARSE UN RATO

Dos filas fijas bajo el tablero: arriba las cifras, debajo las notas a lápiz. Sin modo que cambiar — tres candidatos son tres toques, y la fila de notas también sirve para ver qué hay en la celda seleccionada.

Pausa detiene el reloj y oculta el tablero. El reloj también se detiene solo en cuanto la app deja de estar en pantalla. La partida en curso sobrevive a cerrar la app: cifras, notas, historial de deshacer, una rama abierta y el tiempo.


CÓDIGO ABIERTO

Apache-2.0. Léelo, compílalo, modifícalo:
https://github.com/alramlechner/Sudomnia
```

## Nota de mantenimiento

La versión en español es una adaptación propia, no una traducción literal. Si
se cambia el contenido de `listing-en.md`, hay que actualizar también este
archivo — la ficha de Play muestra los idiomas por separado, y una
desincronización se nota.

## Categorización, Data Safety, recursos gráficos

Están en `listing-en.md` y valen para todos los idiomas. La gráfica de
funciones (feature graphic) existe por idioma:
`store/play-feature-1024x500-es.png`.
