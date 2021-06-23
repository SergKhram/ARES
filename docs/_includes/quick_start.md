<h1 id="quick-start" style="color:#333;">Quick start</h1>
<h3 style="color:#444;">Add maven central repo</h3>
<div class="language-plaintext highlighter-rouge"><div class="highlight"><pre class="highlight"><code>repositories {
  mavenCentral()
}
</code></pre></div></div>
<h3 style="color:#444;">Add ARES plugin(basic)</h3>
<div class="language-plaintext highlighter-rouge"><div class="highlight"><pre class="highlight"><code>implementation 'io.github.sergkhram:ares-plugin:1.2.0-RELEASE'
</code></pre></div></div>
<h4 style="color:#444;">Or ARES execution plugin</h3>
<div class="language-plaintext highlighter-rouge"><div class="highlight"><pre class="highlight"><code>implementation 'io.github.sergkhram:ares-exec-plugin:1.2.0-RELEASE'
</code></pre></div></div>
<h3 style="color:#444;">Apply ARES plugin</h3>
<div class="language-plaintext highlighter-rouge"><div class="highlight"><pre class="highlight"><code>plugins {
  id 'io.github.sergkhram.aresPlugin'
}
</code></pre></div></div>
<h4 style="color:#444;">Or ARES execution plugin</h3>
<div class="language-plaintext highlighter-rouge"><div class="highlight"><pre class="highlight"><code>plugins {
  id 'io.github.sergkhram.aresExecPlugin'
}
</code></pre></div></div>
