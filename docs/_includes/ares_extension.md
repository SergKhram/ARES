<h1 id="ares-extension" style="color:#333;">ARES extension</h1>
  <p>You can use <code class="language-plaintext highlighter-rouge">ares {...}</code> in your build.gradle(:app) and set the configuration as extension. For example:</p>
  <div class="language-plaintext highlighter-rouge"><div class="highlight"><pre class="highlight"><code>ares {
    enrichBy = "MARATHON"
    marathonBlock {
        buildType = "debug"
    }
    startAsyncResultFilesTransferFrom = 100
    startAsyncOtherFilesTransferFrom = 300
    asyncFilesTransferThreadsCount = 20
}
</code></pre></div>  </div>
  <p>or</p>
  <div class="language-plaintext highlighter-rouge"><div class="highlight"><pre class="highlight"><code>ares {
    enrichBy = "CLEAN_ALLURE"
    allureBlock {
        deviceSerials = "emulator-5554,emulator-5556"
    }
    startAsyncResultFilesTransferFrom = 300
    startAsyncOtherFilesTransferFrom = 1000
    asyncFilesTransferThreadsCount = 50
}
</code></pre></div>  </div>
<p><strong>root</strong> properties:</p>
<blockquote>
<p>enrichBy - String (MARATHON || CLEAN_ALLURE)</p>
<p>startAsyncResultFilesTransferFrom - Integer</p>
<p>startAsyncOtherFilesTransferFrom - Integer</p>
<p>asyncFilesTransferThreadsCount - Integer</p>
<p>androidHome - String</p>
</blockquote>
<p><strong>marathonBlock</strong> properties:</p>
<blockquote>
<p>screenRecordType - String (VIDEO || SCREENSHOT)</p>
<p>buildType - String</p>
<p>isMarathonCLI - Boolean</p>
<p>reportDirectory - String</p>
</blockquote>

<p><strong>allureBlock</strong> properties:</p>
<blockquote>
  <p>remoteAllureFolder - String</p>
 <p>deviceSerials - String with delimiter ‘,’</p>
</blockquote>

<h2 id="ares-extension" style="color:#444;">ARES execution plugin</h2>
<div class="language-plaintext highlighter-rouge"><div class="highlight"><pre class="highlight"><code>ares {
    enrichBy = "MARATHON"
    marathonBlock {
        buildType = "debug"
    }
    startAsyncResultFilesTransferFrom = 100
    startAsyncOtherFilesTransferFrom = 300
    asyncFilesTransferThreadsCount = 20
    testExecutionBlock {
        executeBy = "MARATHON"
        executionIgnoreFailures = false
    }
}
</code></pre></div>  </div>
<p><strong>testExecutionBlock</strong> properties:</p>
<blockquote>
  <p>executeBy - String</p>
 <p>executionIgnoreFailures - Boolean</p>
</blockquote>
