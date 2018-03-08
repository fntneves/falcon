<template>
  <div id="drawing">
    <div v-if="universe">
      <!-- <button @click="back" :disabled="bigbang">Back</button> -->
      <button @click="tick" :disabled="blackhole">Tick</button>
    </div>

    <input type="file" name="file" @change="readFile">
    <h5>Clock: {{ this.clock }}</h5>
  </div>
</template>

<script>
import SVG from 'svg.js';
import EventUniverse from '../core/EventUniverse';
import TraceDrawer from '../core/drawer/TraceDrawer';
import FileReaderPromise from '../core/util/FileReaderPromise';

export default {
  mounted() {
    this.$on('tick', () => {
      this.drawer.nextClock();
    });

    this.drawer = null;
  },
  data() {
    return {
      clock: 0,
      universe: null,
    };
  },
  methods: {
    tick() {
      this.clock += 1;
      this.$emit('tick', this.clock);
    },
    back() {
      this.clock -= this.clock > 0 ? 1 : 0;
      this.$emit('tick', this.clock);
    },
    readFile(e) {
      const file = e.target.files[0];

      new FileReaderPromise(file).readAsText()
        .then((content) => {
          const jsonContent = JSON.parse(content);
          this.universe = new EventUniverse(jsonContent);
          this.drawer = new TraceDrawer(SVG('drawing'), this.universe);
          this.back();
        });
    },
  },
  computed: {
    bigbang() {
      return this.clock === 0;
    },
    blackhole() {
      return this.clock === this.universe.maxClock;
    },
  },
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h1, h2 {
  font-weight: normal;
}

ul {
  list-style-type: none;
  padding: 0;
}

li {
  display: inline-block;
  margin: 0 10px;
}

a {
  color: #42b983;
}
</style>
