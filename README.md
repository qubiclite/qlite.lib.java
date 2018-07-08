# Qubic Lite Library

This library provides a basic framework to interact with the Qubic Lite protocol.



## Installation

### 1) Requirements

You will need the JDK and Maven. A guide on how to do that on Windows or Ubuntu can be found in [this section](https://github.com/mikrohash/isf-jclient#1-get-the-requirements-jdk--maven).

### 2) Dependencies

To make this library work, you have to install the [iota java library](https://github.com/iotaledger/iota.lib.java). In the following sections we will use git to clone the repositories. But you can also download them [manually](https://github.com/mikrohash/isf-jclient#method-c-manual-download) or [via wget](https://github.com/mikrohash/isf-jclient#method-a-download-via-wget).

#### IOTA Library

    cd /path/to/your/favourite/directory/
    git clone https://github.com/iotaledger/iota.lib.java
    cd iota.lib.java/
    mvn install
    
### 3) Building the Qlite Library

    cd /path/to/your/favourite/directory/
    git clone https://github.com/qubiclite/qlite.lib.java
    cd qlite.lib.java/  
    mvn versions:use-latest-versions -DallowSnapshots=true -DexcludeReactor=false
    mvn install
    
## Adding the Maven Module

After you have installed the maven module as shown above,
you can include it into your own maven project by adding
it as a dependency to your pom.xml file:

    <dependency>
        <groupId>org.qubiclite</groupId>
        <artifactId>qlite.lib.java</artifactId>
        <version>0.1-SNAPSHOT</version>
    </dependency>
    
## Using the Library

### Starting a Qubic

Here is a small example showcasing how you can publish a new qubic:

    String code = "return(epoch^2);";
    
    // start qubic in 180 seconds
    int executionStart = (int)(System.currentTimeMillis()/1000)+180;

    QubicWriter qw = new QubicWriter(executionStart, 30, 30, 10);
    qw.setCode(code);
    qw.publishQubicTx();
    
Now go tell your friends to set up their oracles (see [next section](#running-an-oracle))
and apply to your qubic. After that we can publish the assembly transaction:

    // the oracle IDs of your friends oracles
    String michelsOracle = "WQTNBLHTEIRTFEIL99NDCGDGDWDYOJVSMYODNLCHGHZRBQKTXVMSTGVOO9C9KMYGJQLYXWPCTLVIHC999";
    String clairesOracle = "HMSCIRTQEM9GMRENWOXEJYXARGZNUTLFBTSORUSFU9LYTOLTGGYJXCYAJYGEDTDZVUXRUTPMPSWDWO999";
    
    // add them to the assembly
    qw.addToAssembly(michelsOracle);
    qw.addToAssembly(clairesOracle);
    
    // publish the assembly transaction (this has to be done within the 180 seconds set in executionStart!)
    qw.publishAssemblyTx();

### Running an Oracle

These few lines will create a new oracle that processes a specific qubic:

    // replace this with the id of the qubic you want to process
    String qubicID = "KAHQRHDKODUBLPCRUBCBEKAQKAAHTCEUPMFRTMDXTKHNJMGVSMHH9T9TBKBAFRGJTGKKIKZM9HWMKX999";
    
    // create a new oracle for the specific qubic
    QubicReader qr = new QubicReader(qubicID);
    OracleWriter ow = new OracleWriter(qr);
    
    // let the oracle run its automized life-cycle asynchronously
    // it will apply for the qubic and process it if it makes it into the assembly
    OracleManager om = new OracleManager(ow);
    om.start();
    
    // you can give this id to the qubic owner, so he can manually add you to the assembly
    String myOracleID = ow.getID();
    
More content will be added soon.

## Project Resources

official project website: http://qubiclite.org

java documentation: http://qubiclite.org/doc/qlite.lib.java